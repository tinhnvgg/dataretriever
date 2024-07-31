package io.github.vatisteve.dataretriever.seatable.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum STVersion {

    BEFORE_4_4("/dtable-server/api/v1/dtables/", true),
    FROM_4_4("/api-gateway/api/v2/dtables/", false);

    private final String dtablePath;
    private final boolean supportConvertKeys;

}
