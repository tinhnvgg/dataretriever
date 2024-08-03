package io.github.vatisteve.dataretriever.seatable.model.metadata;

import java.util.List;

public record STButton (String name, String color, List<STButtonAction> actions) implements STDataType {
    public record STButtonAction (String type) {}
}
