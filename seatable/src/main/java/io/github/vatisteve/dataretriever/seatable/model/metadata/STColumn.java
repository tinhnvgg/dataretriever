package io.github.vatisteve.dataretriever.seatable.model.metadata;

public record STColumn (
        String key,
        String type,
        String name,
        String description,
        STDataType data
) {}
