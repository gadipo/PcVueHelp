package com.gadipo.pcvue.xmlbulk.core;

import java.util.List;

public record EditResult(String outputXml, int matchedNodeCount, List<ChangeRecord> changes) {
    public EditResult {
        changes = List.copyOf(changes);
    }
}
