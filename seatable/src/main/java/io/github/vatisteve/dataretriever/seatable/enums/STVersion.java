package io.github.vatisteve.dataretriever.seatable.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum STVersion {

    BEFORE_4_4("/dtable-server/api/v1/dtables/", "error_msg", false),
    FROM_4_4("/api-gateway/api/v2/dtables/", "error_message", true);

    private final String tablePath;
    private final String errorMessageKey;
    private final boolean supportConvertKeys;

}
