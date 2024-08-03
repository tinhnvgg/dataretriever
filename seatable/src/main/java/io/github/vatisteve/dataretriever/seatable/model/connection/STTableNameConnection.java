package io.github.vatisteve.dataretriever.seatable.model.connection;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class STTableNameConnection extends STConnection {
    private String tableName;
    private long startRow;
    private int limit;
    public boolean shouldQueryAll() {
        return limit == 0;
    }
}
