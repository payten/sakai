package org.sakaiproject.pasystem.tool;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.sakaiproject.pasystem.api.PASystem;
import org.sakaiproject.pasystem.api.Popup;


@Data
@AllArgsConstructor(access = AccessLevel.PROTECTED)
class PopupForm {

    private String uuid;
    private String descriptor;
    private long startTime;
    private long endTime;
    private boolean isOpenCampaign;
    private List<String> assignToUsers;
    private String templateFile;


    public static PopupForm fromPopup(Popup existingPopup, PASystem paSystem) {
        String uuid = existingPopup.getUuid();
        List<String> assignees = paSystem.getPopups().getAssignees(uuid);

        return new PopupForm(uuid, existingPopup.getDescriptor(),
                             existingPopup.getStartTime(),
                             existingPopup.getEndTime(),
                             existingPopup.isOpenCampaign(),
                             assignees,
                             null);
    }



    public Popup toPopup() {
        return Popup.create(descriptor, startTime, endTime, isOpenCampaign);
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


    public static PopupForm fromRequest(String uuid, HttpServletRequest request) {
        String descriptor = request.getParameter("descriptor");
        boolean isOpenCampaign = "open-campaign".equals(request.getParameter("open-campaign"));
        long startTime = parseTime(request.getParameter("start_time_selected_datetime"));
        long endTime = parseTime(request.getParameter("end_time_selected_datetime"));

        List<String> assignToUsers = new ArrayList<String>();
        if (request.getParameter("distribution") != null) {
            for (String user : request.getParameter("distribution").split("[\r\n]+")) {
                if (!user.isEmpty()) {
                    assignToUsers.add(user);
                }
            }
        }

        return new PopupForm(uuid, descriptor, startTime, endTime, isOpenCampaign, assignToUsers, null);
    }


    public boolean hasValidStartTime() { return startTime >= 0; }

    public boolean hasValidEndTime() { return endTime >= 0; }

    public boolean startTimeBeforeEndTime() {
        if (startTime < 0 || endTime < 0) {
            return true;
        } else {
            return (startTime <= endTime);
        }
    }
}

