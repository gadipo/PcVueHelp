package com.gadipo.pcvue.xmlbulk.core;

public record SearchCriteria(
        String tagName,
        String attributeName,
        String attributeValueContains,
        String textContains,
        String textRegex,
        String xpathContains,
        String xpathExpression) {
}
