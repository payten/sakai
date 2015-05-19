package org.sakaiproject.pasystem.api;

import java.io.InputStream;
import java.util.Date;

public interface Popups {

    public String createCampaign(String descriptor, Date startDate, Date endDate, InputStream templateContent);

    public void openCampaign(String id);

    public boolean hasCampaign(String descriptor);
}
    
