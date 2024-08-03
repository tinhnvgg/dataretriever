package io.github.vatisteve.dataretriever.seatable;

import io.github.vatisteve.dataretriever.seatable.enums.STVersion;
import io.github.vatisteve.dataretriever.seatable.model.connection.STTableNameConnection;
import io.github.vatisteve.dataretriever.seatable.model.metadata.STColumn;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class STTableNameQueryTest {

    @BeforeAll
    static void beforeAll() {
        log.info("Starting test...");
    }

    @Test
    @DisplayName("Request to get SeaTable column list")
    void getColumns() throws IOException, InterruptedException {
        STTableNameConnection connection = STTableNameConnection.builder()
                .url("https://cloud.seatable.io")
                .apiKey("ceabb258ff20693b3bd5ee3d2c841cdddf6c333b")
                .version(STVersion.FROM_4_4)
                .tableName("table 1")
                .build();
        STTableNameQuery query = new STTableNameQuery(connection);
        List<STColumn> columns = query.getColumns();
        assertNotNull(columns);
        int i = 0;
        log.info("Found {} columns", columns.size());
        for (STColumn column : columns) {
            log.info("Column #{} - {} - {}", ++i, column.name(), column.type());
        }
    }
}