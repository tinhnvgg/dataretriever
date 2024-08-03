package io.github.vatisteve.dataretriever.seatable.model.metadata;

public record STNumber(
        String format,
        byte precision,
        boolean enablePrecision,
        String decimal,
        String thousands,
        int formatMinValue,
        int formatMaxValue
) implements STDataType {}
