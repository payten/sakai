package org.sakaiproject.pasystem.api;

import java.util.Date;
import java.io.InputStream;

public interface Popups {

    public String createCampaign(String descriptor, Date startDate, Date endDate, InputStream templateContent);

    public void openCampaign(String id);

    public boolean hasCampaign(String descriptor);

    public String getFooter();
}
    
