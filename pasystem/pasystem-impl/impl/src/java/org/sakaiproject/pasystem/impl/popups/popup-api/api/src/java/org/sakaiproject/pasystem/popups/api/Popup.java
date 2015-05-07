package org.sakaiproject.pasystem.popups.api;

import java.io.IOException;

public interface Popup {
    public boolean isActive();
    public String getCampaign();
    public String getTemplate() throws IOException;
}
