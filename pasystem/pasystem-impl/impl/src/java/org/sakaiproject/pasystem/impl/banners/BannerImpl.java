package org.sakaiproject.pasystem.impl.banners;

import lombok.Getter;
import org.sakaiproject.pasystem.api.Banner;

import java.util.Arrays;
import java.util.Date;


public class BannerImpl implements Banner {
    @Getter
    private String uuid;
    @Getter
    private String message;
    @Getter
    private long startTime;
    @Getter
    private long endTime;
    @Getter
    private String hosts;

    private boolean isActive;
    private boolean isDismissible;


    public BannerImpl(String uuid, String message, String hosts, int dismissible, int active, long startTime, long endTime) {
        this.uuid = uuid;
        this.message = message;
        this.hosts = hosts;
        this.isActive = (active == 1);
        this.isDismissible = (dismissible == 1);
        this.startTime = startTime;
        this.endTime = endTime;
    }


    public boolean isActiveNow() {
        if (!isActive()) {
            return false;
        }

        if (startTime == 0 && endTime == 0) {
            return isActive();
        }

        Date now = new Date();

        return (now.after(new Date(startTime))
                && (endTime == 0 || now.before(new Date(endTime))));
    }


    public boolean isDismissible() {
        return this.isDismissible;
    }


    public boolean isActive() {
        return this.isActive;
    }


    public boolean isActiveForHost(String hostname) {
        // are we active?
        if (!isActiveNow()) {
            return false;
        }

        // if no hosts then assume active for any host
        if (hosts == null || hosts.isEmpty()) {
            return true;
        }

        // we have some hosts defined, so check if the current is listed
        return Arrays.asList(hosts.split(",")).contains(hostname);
    }
}
