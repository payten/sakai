package org.sakaiproject.pasystem.api;

import lombok.Getter;

public class Popup {

    @Getter private String uuid;
    @Getter private String descriptor;
    @Getter private long startTime;
    @Getter private long endTime;

    private String template;


    public static Popup createNullPopup() {
        return new Popup();
    }

    public static Popup createPopup(String uuid, String descriptor, long startTime, long endTime) {
        return createPopup(uuid, descriptor, startTime, endTime, null);
    }

    public static Popup createPopup(String uuid, String descriptor, long startTime, long endTime, String template) {
        return new Popup(uuid, descriptor, startTime, endTime, template);
    }

    private Popup() {
        this.uuid = null;
    }

    private Popup(String uuid, String descriptor, long startTime, long endTime, String template) {
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
