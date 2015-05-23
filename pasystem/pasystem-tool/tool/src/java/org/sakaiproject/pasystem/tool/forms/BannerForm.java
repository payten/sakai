package org.sakaiproject.pasystem.tool.forms;

import lombok.Data;
import org.sakaiproject.pasystem.api.Banner;
import javax.servlet.http.HttpServletRequest;
import org.sakaiproject.pasystem.tool.handlers.ErrorReporter;


@Data
public class BannerForm extends BaseForm {

    private String message;
    private String hosts;
    private boolean isActive;
    private boolean isDismissible;


    private BannerForm(String uuid, String message, String hosts, long startTime, long endTime, boolean isActive, boolean isDismissable) {
        this.uuid = uuid;
        this.message = message;
        this.hosts = hosts;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = isActive;
        this.isDismissible = isDismissible;
    }


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


    public void validate(ErrorReporter errors) {
        if (!hasValidStartTime()) {
            errors.addError("start_time", "invalid_time");
        }

        if (!hasValidEndTime()) {
            errors.addError("end_time", "invalid_time");
        }

        if (!startTimeBeforeEndTime()) {
            errors.addError("start_time", "start_time_after_end_time");
            errors.addError("end_time", "start_time_after_end_time");
        }
    }
}

