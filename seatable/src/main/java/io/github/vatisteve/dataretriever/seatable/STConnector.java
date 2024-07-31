package io.github.vatisteve.dataretriever.seatable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.vatisteve.dataretriever.seatable.model.STBaseInfo;
import io.github.vatisteve.dataretriever.seatable.model.STConnectionInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

/**
 * @author tinhnv - Jul 26 2024
 * @see <a href='https://api.seatable.io/reference/limits'>About SeaTable API limitation</a>
 * @see <a href='https://docs.google.com/spreadsheets/d/1DpPbZ9GJThAHFY5A10AQ5Bo9WoTrGuv9JBAnFnfAODo/edit?gid=1537567090#gid=1537567090'>Document</a>
 */
@Slf4j
public abstract class STConnector {

    protected final HttpClient client;
    @Getter
    protected STBaseInfo baseInfo;
    protected ObjectMapper mapper = new ObjectMapper();

    public static final String SEA_TABLE_PROPERTIES_PREFIX = "_";
    protected final UnaryOperator<String> stAuth = s -> String.format("Bearer %s", s);

    protected STConnector(STConnectionInfo connectionInfo) throws IOException, InterruptedException {
        log.debug("Init connection to {} SeaTable version {}", connectionInfo.getUrl(), connectionInfo.getVersion());
        client  = HttpClient.newHttpClient();
        // add http client configuration if needed
        //...
        initBaseInfo(connectionInfo);
    }

    /**
     * @see <a href='https://api.seatable.io/reference/getbasetokenwithapitoken'>Get Base token with API token</a>
     */
    public void initBaseInfo(STConnectionInfo connectionInfo) throws IOException, InterruptedException {
        URI uri = URI.create(connectionInfo.getUrl()).resolve("/api/v2.1/dtable/app-access-token/");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header("Accept", "application/json")
            .header("Authorization", stAuth.apply(connectionInfo.getApiKey()))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected response code when getting base info: " + response.statusCode());
        }
        log.trace("Base info response: {}", response.body());
        this.baseInfo = responseToBaseInfo(response.body());
    }

    private STBaseInfo responseToBaseInfo(String baseInfoInString) throws JsonProcessingException {
        JsonNode node = mapper.readTree(baseInfoInString);
        String accessToken = node.get("access_token").asText();
        String baseUuid = node.get("dtable_uuid").asText();
        String baseName = node.get("dtable_name").asText();
        return new STBaseInfo(accessToken, baseUuid, baseName);
    }

    protected abstract CompletableFuture<ArrayNode> requestData();
    protected abstract ArrayNode detachResponse(String response);
    protected abstract CompletableFuture<String> transformData();

    public CompletableFuture<String> getData() {
        return transformData(); // by default
    }

    /**
     * NOTE: Inefficient use of memory
     * <br />
     * Should use the {@link #getData()} instead
     * <br />
     * Catch the {@link java.util.concurrent.CompletionException}
     */
    public String getDataAsString() {
        return getData().join();
    }


    protected static JsonNode removeSeaTableProperties(JsonNode n, Set<String> keys) {
        if (keys.isEmpty()) {
            n.fieldNames().forEachRemaining(f -> {
                if (!f.startsWith(SEA_TABLE_PROPERTIES_PREFIX)) keys.add(f);
            });
        }
        return ((ObjectNode) n).retain(keys);
    }

    public static class STConnectException extends RuntimeException {
        public STConnectException(int statusCode) {
            super("Error requesting to SeaTable server with status code " + statusCode);
        }
        public STConnectException(String message) {
            super(message);
        }
        @Serial
        private static final long serialVersionUID = -177508340932532651L;
    }

}
