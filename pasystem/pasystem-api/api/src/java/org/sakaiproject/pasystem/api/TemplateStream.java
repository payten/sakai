package org.sakaiproject.pasystem.api;

import java.io.InputStream;
import java.io.IOException;
import lombok.Data;

@Data
public class TemplateStream {
    private final InputStream inputStream;
    private final long length;
}
