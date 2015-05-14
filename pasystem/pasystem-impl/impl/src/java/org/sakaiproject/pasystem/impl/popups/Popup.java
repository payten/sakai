package org.sakaiproject.pasystem.impl.popups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.FileReader;
import java.io.File;

import org.sakaiproject.component.cover.ServerConfigurationService;

class Popup {

    private static final Logger LOG = LoggerFactory.getLogger(Popup.class);

    private String campaign;
    private String template;

    public static Popup createNullPopup() {
        return new Popup(null, null);
    }

    public static Popup createPopup(String campaign, String template) {
        return new Popup(campaign, template);
    }

    private Popup(String campaign, String template) {
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
