package org.sakaiproject.gradebookng.tool.actions;

import com.fasterxml.jackson.databind.JsonNode;

public interface Action {
    public ActionResponse handleEvent(JsonNode params);
}
