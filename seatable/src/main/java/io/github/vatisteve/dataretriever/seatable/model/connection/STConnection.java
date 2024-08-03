package io.github.vatisteve.dataretriever.seatable.model.connection;

import io.github.vatisteve.dataretriever.seatable.enums.STVersion;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public abstract class STConnection {
    private String url;
    private String apiKey;
    private STVersion version;
}
