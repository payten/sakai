package org.sakaiproject.pasystem.tool;

import lombok.Value;

@Value
public class Error {
    private final String field;
    private final String errorCode;
    private final String[] values;
}
