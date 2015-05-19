package org.sakaiproject.pasystem.impl.popups;

import org.sakaiproject.pasystem.api.Popup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

class PopupImpl implements Popup {

    private static final Logger LOG = LoggerFactory.getLogger(Popup.class);

    @Getter
    private String uuid;

    @Getter
    private String descriptor;

    @Getter
    private long startTime;

    @Getter
    private long endTime;

    private String template;


    public static Popup createNullPopup() {
        return new PopupImpl();
    }

    public static Popup createPopup(String uuid, String descriptor, long startTime, long endTime) {
        return createPopup(uuid, descriptor, startTime, endTime, null);
    }

    public static Popup createPopup(String uuid, String descriptor, long startTime, long endTime, String template) {
        return new PopupImpl(uuid, descriptor, startTime, endTime, template);
    }

    private PopupImpl() {
        this.uuid = null;
    }

    private PopupImpl(String uuid, String descriptor, long startTime, long endTime, String template) {
        this.uuid = uuid;
        this.descriptor = descriptor;
        this.startTime = startTime;
        this.endTime = endTime;
        this.template = template;
    }

    public boolean isActive() {
        long now = System.currentTimeMillis();
        return (uuid != null) && startTime <= now && now <= endTime;
    }

    public String getTemplate() {
        if (template == null) {
            throw new RuntimeException("Template not loaded for Popup instance");
        }

        return template;
    }
}
