package io.github.vatisteve.dataretriever.seatable.model.metadata;

public record STColumn (
        String key,
        String name,
        String type,
        String description,
        STDataType dataType
) {}
