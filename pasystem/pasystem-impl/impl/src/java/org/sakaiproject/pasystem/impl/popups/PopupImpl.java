package org.sakaiproject.pasystem.impl.popups;

import org.sakaiproject.pasystem.api.Popup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class PopupImpl implements Popup {

    private static final Logger LOG = LoggerFactory.getLogger(Popup.class);

    private String campaign;
    private String template;

    public static Popup createNullPopup() {
        return new PopupImpl(null, null);
    }

    public static Popup createPopup(String campaign, String template) {
        return new PopupImpl(campaign, template);
    }

    private PopupImpl(String campaign, String template) {
        this.campaign = campaign;
        this.template = template;
    }

    public boolean isActive() {
        return (campaign != null);
    }

    public String getCampaign() {
        return campaign;
    }

    public String getTemplate() {
        return template;
    }
}
