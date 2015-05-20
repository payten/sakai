package org.sakaiproject.pasystem.tool;

import lombok.Getter;
import lombok.ToString;

@ToString
public class Error {

    @Getter private String field;
    @Getter private String errorCode;
    @Getter private String[] values;

    public Error (String field, String errorCode, String[] values) {
        this.field = field;
        this.errorCode = errorCode;
        this.values = values;
    }
   
}
