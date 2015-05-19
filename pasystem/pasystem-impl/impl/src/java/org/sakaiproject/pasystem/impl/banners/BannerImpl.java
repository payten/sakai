package org.sakaiproject.pasystem.impl.banners;

import java.util.Arrays;
import java.util.Date;

import org.sakaiproject.pasystem.api.Banner;

public class BannerImpl implements Banner {
  private String uuid;
  private String message;

  private String hosts;
  private boolean isActive;
  private boolean isDismissible;
  private long activeFrom;
  private long activeUntil;

  public BannerImpl(String uuid, String message, String hosts, int dismissible, int active, long activeFrom, long activeUntil) {
    this.uuid = uuid;
    this.message = message;
    this.hosts = hosts;
    this.isActive = (active == 1);
    this.isDismissible = (dismissible == 1);
    this.activeFrom = activeFrom;
    this.activeUntil = activeUntil;
  }

  public String getUuid() {
    return uuid;
  }

  public String getMessage() {
    return message;
  }

  public boolean isActive() {
    if (activeFrom == 0 && activeUntil == 0) {
      return isActive;
    }

    Date now = new Date();

    return (now.after(new Date(activeFrom))
            && (activeUntil == 0 || now.before(new Date(activeUntil))));
  }

  public boolean isDismissible() {
    return this.isDismissible;
  }

  public boolean isActiveForHost(String hostname) {
    // are we active?
    if (!isActive()) {
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
