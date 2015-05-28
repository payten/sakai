package org.sakaiproject.pasystem.api;

import org.sakaiproject.pasystem.api.Acknowledger;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface Popups extends Acknowledger {

    public String createCampaign(Popup popup,
                                 TemplateStream templateContent,
                                 Optional<List<String>> assignToUsers);

    public boolean updateCampaign(final String uuid,
                                  Popup popup,
                                  Optional<TemplateStream> templateInput,
                                  Optional<List<String>> assignToUsers);

    public List<Popup> getAll();

    public String getPopupContent(final String uuid);

    public Optional<Popup> getForId(final String uuid);

    public List<String> getAssignees(final String uuid);

    public boolean deleteCampaign(final String uuid);
}
