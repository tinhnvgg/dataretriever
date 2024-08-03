package io.github.vatisteve.dataretriever.seatable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.vatisteve.dataretriever.seatable.model.connection.STTableNameConnection;
import io.github.vatisteve.dataretriever.seatable.model.metadata.STColumn;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author tinhnv - Jul 26 2024
 * @see <a href='https://api.seatable.io/reference/listrows'>List Rows</a>
 */
@Slf4j
@Getter
public class STTableNameQuery extends STConnector {

    private final int defaultBatchSize;

    private final STTableNameConnection connectionInfo;

    public STTableNameQuery(STTableNameConnection connectionInfo) throws IOException, InterruptedException {
        this(connectionInfo, 1000); /* limit of SeaTable api response */
    }

    public STTableNameQuery(STTableNameConnection connectionInfo, int defaultBatchSize) throws IOException, InterruptedException {
        super(connectionInfo);
        this.connectionInfo = connectionInfo;
        this.defaultBatchSize = defaultBatchSize;
    }

    @Override
    protected CompletableFuture<ArrayNode> requestData() {
        CompletableFuture<ArrayNode> response;
        if (connectionInfo.shouldQueryAll()) {
            log.debug("Querying all data from table {}", connectionInfo.getTableName());
            response = doRequest(0, defaultBatchSize).thenCompose(data -> {
                log.trace("Request data for each batch of {}. Start!", defaultBatchSize);
                if (data.isNull() || data.isEmpty()) return CompletableFuture.completedFuture(data);
                log.trace("Request data for each batch of {}. Second batch!", defaultBatchSize);
                return processNextBatchWithoutLimit(data, defaultBatchSize);
            });
        } else {
            long offset = connectionInfo.getStartRow();
            int limit = connectionInfo.getLimit();
            log.debug("Querying data from table {} with offset {} and limit {}", connectionInfo.getTableName(), offset, limit);
            if (limit - offset <= defaultBatchSize) {
                response = doRequest(offset, limit);
            } else {
                response = doRequest(offset, defaultBatchSize)
                    .thenCompose(data -> processNextBatchWithLimit(data, offset + defaultBatchSize, offset + limit));
            }
        }
        return response;
    }

    @Override
    protected CompletableFuture<String> transformData() {
        return requestData().thenApply(ArrayNode::toString);
    }

    private CompletionStage<ArrayNode> processNextBatchWithoutLimit(ArrayNode nodes, int offset) {
        log.trace("Request next batch with offset {}", offset);
        return doRequest(offset, defaultBatchSize).thenCompose(data -> {
            if (data.isNull() || data.isEmpty()) return CompletableFuture.completedFuture(nodes);
            log.trace("Continue to next batch with offset {}", offset);
            nodes.addAll(data);
            return processNextBatchWithoutLimit(nodes, offset + defaultBatchSize);
        });
    }

    private CompletionStage<ArrayNode> processNextBatchWithLimit(ArrayNode nodes, long offset, long maxOffset) {
        log.trace("Request next batch with offset {}, maxOffset {}", offset, maxOffset);
        if (offset >= maxOffset) return CompletableFuture.completedFuture(nodes);
        int batchSize = (int) (offset + defaultBatchSize < maxOffset ? defaultBatchSize : maxOffset);
        return doRequest(offset, batchSize).thenCompose(data -> {
            if (data.isNull() || data.isEmpty()) return CompletableFuture.completedFuture(nodes);
            log.trace("Continue to next batch with offset {} to maxOffset {}", offset, maxOffset);
            nodes.addAll(data);
            return processNextBatchWithLimit(nodes, offset + defaultBatchSize, maxOffset);
        });
    }

    private CompletableFuture<ArrayNode> doRequest(long start, int limit) {
        // should use URI builder
        String uriFormat = "%s%s%s/rows/?table_name=%s&start=%d&limit=%d";
        if (connectionInfo.getVersion().isSupportConvertKeys()) {
            uriFormat = uriFormat + "&convert_keys=true";
        }
        URI uri = URI.create(
            String.format(uriFormat,
                    connectionInfo.getUrl(), connectionInfo.getVersion().getTablePath(),
                    baseInfo.uuid(), encode(connectionInfo.getTableName()), start, limit
            )
        );
        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header("Accept", "application/json")
            .header("Authorization", stAuth(baseInfo.token()))
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(res -> {
                if (res.statusCode() == 200) return res;
//                if (res.statusCode() == 401) {} TODO handle un-authorization status code
                throw handleErrorResponse(res);
            })
            .thenApply(HttpResponse::body)
            .thenApply(this::detachResponse);
    }

    @Override
    protected ArrayNode detachResponse(String response) {
        try {
            ArrayNode result = mapper.createArrayNode();
            JsonNode resNode = mapper.readTree(response);
            Set<String> selectKeys = new HashSet<>();
            resNode.get("rows").elements().forEachRemaining(n -> {
                n = removeSeaTableProperties(n, selectKeys);
                result.add(n);
            });
            return result;
        } catch (JsonProcessingException e) {
            log.error("Error occurred when processing JSON data: {}", e.getMessage(), e);
            throw new STConnectException("Error occurred when processing JSON data: " + e.getMessage());
        }
    }

    /**
     * @see <a href='https://api.seatable.io/reference/listcolumns-1'>List columns</a>
     * @return List of {@link STColumn}
     */
    public List<STColumn> getColumns() {
        String uriFormat = "%s%s%s/columns/?table_name=%s";
        URI uri = URI.create(String.format(uriFormat,
                connectionInfo.getUrl(),
                connectionInfo.getVersion().getTablePath(),
                baseInfo.uuid(),
                encode(connectionInfo.getTableName())
        ));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .header("Authorization", stAuth(baseInfo.token()))
                .GET()
                .build();
        try {
            return columnsResponse(client.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException e) {
            log.error("Error occurred when calling get column list api: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Interrupted exception occurred: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        return Collections.emptyList();
    }

    private List<STColumn> columnsResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            log.error("Request columns from table {} with the unexpected response: \nSTATUS{}\n{}",
                    connectionInfo.getTableName(), response.statusCode(), response.body());
            return Collections.emptyList();
        }
        try {
            JsonNode columnsNode = mapper.readTree(response.body()).get("columns");
            return mapper.treeToValue(columnsNode, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("Couldn't parse JSON data: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
