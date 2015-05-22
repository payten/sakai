package org.sakaiproject.pasystem.api;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface Popups {

    public String createCampaign(Popup popup,
                                 InputStream templateContent,
                                 Optional<List<String>> assignToUsers);

    public boolean updateCampaign(final String uuid,
                                  Popup popup,
                                  Optional<InputStream> templateInput,
                                  Optional<List<String>> assignToUsers);

    public List<Popup> getAll();

    public String getPopupContent(final String uuid);

    public Optional<Popup> getForId(final String uuid);

    public List<String> getAssignees(final String uuid);

    public boolean deleteCampaign(final String uuid);

    public void acknowledge(String uuid, String userEid, String acknowledgementType);
}
