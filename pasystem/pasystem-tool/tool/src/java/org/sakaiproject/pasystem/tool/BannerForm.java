package org.sakaiproject.pasystem.tool;

import lombok.Data;
import org.sakaiproject.pasystem.api.Banner;

import javax.servlet.http.HttpServletRequest;


@Data
class BannerForm extends BaseForm {

    private String message;
    private String hosts;
    private boolean isActive;
    private boolean isDismissible;


    public static BannerForm fromBanner(Banner existingBanner) {
        String uuid = existingBanner.getUuid();

        return new BannerForm(uuid,
                existingBanner.getMessage(),
                existingBanner.getHosts(),
                existingBanner.getStartTime(),
                existingBanner.getEndTime(),
                existingBanner.isActive(),
                existingBanner.isDismissible());
    }


    public static BannerForm fromRequest(String uuid, HttpServletRequest request) {
        String message = request.getParameter("message");
        String hosts = request.getParameter("hosts");

        long startTime = "".equals(request.getParameter("start_time")) ? 0 : parseTime(request.getParameter("start_time_selected_datetime"));
        long endTime = "".equals(request.getParameter("end_time")) ? 0 : parseTime(request.getParameter("end_time_selected_datetime"));

        boolean isActive = "on".equals(request.getParameter("active"));
        boolean isDismissible = "on".equals(request.getParameter("dismissible"));

        return new BannerForm(uuid, message, hosts, startTime, endTime, isActive, isDismissible);
    }


    private BannerForm(String uuid, String message, String hosts, long startTime, long endTime, boolean isActive, boolean isDismissable) {
        this.uuid = uuid;
        this.message = message;
        this.hosts = hosts;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = isActive;
        this.isDismissible = isDismissible;
    }
}

