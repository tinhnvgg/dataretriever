package io.github.vatisteve.dataretriever.seatable.model.connection;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class STSqlQueryConnection extends STConnection {
    private String query;
}
