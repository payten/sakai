package org.sakaiproject.pasystem.popups.impl;

import org.sakaiproject.pasystem.popups.api.Popup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.FileReader;
import java.io.File;

import org.sakaiproject.component.cover.ServerConfigurationService;

class PopupImpl implements Popup {

    private static final Logger LOG = LoggerFactory.getLogger(PopupImpl.class);

    private String campaign;
    private String template;
    private String customTemplatePath;

    public static Popup createNullPopup() {
        return new PopupImpl(null, null);
    }

    public static Popup createPopup(String campaign, String template) {
        return new PopupImpl(campaign, template);
    }

    private PopupImpl(String campaign, String template) {
        this.campaign = campaign;
        this.template = template;

        this.customTemplatePath = ServerConfigurationService.getString("popup.template.location", null);
    }

    public boolean isActive() {
        return (campaign != null);
    }

    public String getCampaign() {
        return campaign;
    }

    public String getTemplate() throws IOException {
        if (customTemplatePath == null) {
            throw new IOException("Template location not set");
        }

        File customTemplate = new File(customTemplatePath, new File(template).getName());

        if (!customTemplate.getCanonicalPath().startsWith(customTemplatePath)) {
            throw new IOException("Template invalid");
        }

        FileReader fh = new FileReader(customTemplate);
        char[] buf = new char[4096];

        StringBuilder result = new StringBuilder((int)customTemplate.length());
        int len;
        while ((len = fh.read(buf)) >= 0) {
            result.append(buf, 0, len);
        }

        return result.toString();
    }

}
