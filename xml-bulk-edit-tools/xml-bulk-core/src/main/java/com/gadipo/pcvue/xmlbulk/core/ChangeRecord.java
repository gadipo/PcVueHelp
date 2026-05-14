package com.gadipo.pcvue.xmlbulk.core;

public record ChangeRecord(String path, String changeType, String beforeValue, String afterValue) {
}
