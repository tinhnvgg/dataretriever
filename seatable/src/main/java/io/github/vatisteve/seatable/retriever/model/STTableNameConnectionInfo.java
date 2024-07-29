package io.github.vatisteve.seatable.retriever.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class STTableNameConnectionInfo extends STConnectionInfo {
    private String tableName;
    private long startRow;
    private int limit;
    public boolean shouldQueryAll() {
        return limit == 0;
    }
}
