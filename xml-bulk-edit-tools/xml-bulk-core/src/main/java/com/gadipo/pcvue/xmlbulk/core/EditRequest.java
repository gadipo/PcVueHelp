package com.gadipo.pcvue.xmlbulk.core;

public record EditRequest(
        SearchCriteria criteria,
        EditOperation operation,
        String replacement,
        String operationAttributeName,
        String operationAttributeValue,
        String nodeXml,
        boolean dryRun) {
}
