package io.github.vatisteve.dataretriever.seatable.model.metadata;

public record STAutoNumber(
        String format,
        int maxUsedAutoNumber,
        int digits,
        String prefixType,
        String prefix
) implements STDataType {}
