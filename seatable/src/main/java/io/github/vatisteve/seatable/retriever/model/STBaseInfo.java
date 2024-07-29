package io.github.vatisteve.seatable.retriever.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class STBaseInfo {
    private String token;
    private String uuid;
    private String name;
}
