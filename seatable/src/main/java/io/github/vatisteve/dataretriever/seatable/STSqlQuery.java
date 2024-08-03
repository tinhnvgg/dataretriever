package io.github.vatisteve.dataretriever.seatable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.vatisteve.dataretriever.seatable.model.connection.STSqlQueryConnection;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author tinhnv - Jul 27 2024
 * @see <a href='https://api.seatable.io/reference/querysql'>Query with SQL</a>
 */
@Slf4j
public class STSqlQuery extends STConnector {

    private final STSqlQueryConnection connectionInfo;

    public STSqlQuery(STSqlQueryConnection connectionInfo) throws IOException, InterruptedException {
        super(connectionInfo);
        this.connectionInfo = connectionInfo;
    }

    @Override
    protected CompletableFuture<ArrayNode> requestData() {
        String urlFormat = "%s%s%s/sql";
        URI uri = URI.create(String.format(urlFormat,
                connectionInfo.getUrl(), connectionInfo.getVersion().getTablePath(), baseInfo.uuid()
        ));
        ObjectNode queryBody = mapper.createObjectNode()
            .put("sql", connectionInfo.getQuery());
        if (connectionInfo.getVersion().isSupportConvertKeys()) {
            queryBody.put("convert_keys", true);
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", stAuth.apply(baseInfo.token()))
            .POST(HttpRequest.BodyPublishers.ofString(queryBody.toString()))
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(res -> {
                if (res.statusCode() == 200) return res;
                if (res.statusCode() != 400) throw new STConnectException(res.statusCode());
                try {
                    JsonNode err = mapper.readTree(res.body()).get("error_message");
                    throw new STConnectException(err.asText());
                } catch (Exception e) {
                    throw new STConnectException("Response from SeaTable server with 400 status." +
                        "Unable to parse error message: " + e.getMessage());
                }
            })
            .thenApply(HttpResponse::body)
            .thenApply(this::detachResponse);
    }

    @Override
    protected ArrayNode detachResponse(String response) {
        try {
            JsonNode res = mapper.readTree(response);
            JsonNode isJoinStatementNode = res.get("is_join_stmt");
            JsonNode resultNode = res.get("results");
            if (!resultNode.isArray()) throw new STConnectException("Invalid response from SeaTable server, results is not an array!");
            if (!isJoinStatementNode.isNull() && isJoinStatementNode.asBoolean()) {
                return joinStatementResult(resultNode.elements(), res.get("metadata"));
            }
            return sqlQueryStatementResult(resultNode.elements());
        } catch (JsonProcessingException e) {
            log.error("Error occurred when processing JSON data: {}", e.getMessage(), e);
            throw new STConnectException("Error occurred when processing JSON data: " + e.getMessage());
        }
    }

    private ArrayNode joinStatementResult(Iterator<JsonNode> resultNodes, JsonNode metadata) {
        Map<String, String> metadataMap = new HashMap<>();
        metadata.elements().forEachRemaining(metadataNode -> {
            String metadataColumnName = metadataNode.get("name").textValue();
            if (metadataColumnName.startsWith(SEA_TABLE_PROPERTIES_PREFIX)) return; // break the forEach loop
            metadataMap.put(
                metadataNode.get("id").textValue(),
                // identity format: table_name.column_name
                metadataNode.get("table_name").textValue() + "." + metadataColumnName
            );
        });
        ArrayNode result = mapper.createArrayNode();
        while (resultNodes.hasNext()) {
            JsonNode row = resultNodes.next();
            ObjectNode data = mapper.createObjectNode();
            metadataMap.forEach((colKey, colName) -> data.set(colName, row.get(colKey)));
            result.add(data);
        }
        return result;
    }

    private ArrayNode sqlQueryStatementResult(Iterator<JsonNode> resultNodes) {
        ArrayNode result = mapper.createArrayNode();
        Set<String> selectKeys = new HashSet<>();
        resultNodes.forEachRemaining(n -> {
            n = removeSeaTableProperties(n, selectKeys);
            result.add(n);
        });
        return result;
    }

    @Override
    protected CompletableFuture<String> transformData() {
        return requestData().thenApply(ArrayNode::toString);
    }

}
