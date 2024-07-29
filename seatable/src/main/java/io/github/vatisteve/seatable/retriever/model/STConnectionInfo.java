package io.github.vatisteve.seatable.retriever.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public abstract class STConnectionInfo {
    private String url;
    private String apiKey;
}
