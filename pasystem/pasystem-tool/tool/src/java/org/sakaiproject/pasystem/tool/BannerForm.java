package org.sakaiproject.pasystem.tool;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.sakaiproject.pasystem.api.Banner;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;


@Data
@AllArgsConstructor(access = AccessLevel.PROTECTED)
class BannerForm {

    private String uuid;
    private String message;
    private String hosts;
    private long startTime;
    private long endTime;
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


    private static long parseTime(String timeString) {
        if (timeString == null || "".equals(timeString)) {
            return 0;
        }

        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(timeString).getTime();
        } catch (ParseException e) {
            return -1;
        }
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


    public boolean hasValidStartTime() {
        return startTime >= 0;
    }

    public boolean hasValidEndTime() {
        return endTime >= 0;
    }

    public boolean startTimeBeforeEndTime() {
        if (startTime <= 0 || endTime <= 0) {
            return true;
        } else {
            return startTime <= endTime;
        }
    }
}

