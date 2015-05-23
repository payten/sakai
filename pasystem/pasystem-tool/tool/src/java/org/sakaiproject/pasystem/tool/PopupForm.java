package org.sakaiproject.pasystem.tool;

import lombok.Data;
import org.sakaiproject.pasystem.api.PASystem;
import org.sakaiproject.pasystem.api.Popup;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;


@Data
class PopupForm extends BaseForm {

    private String descriptor;
    private boolean isOpenCampaign;
    private List<String> assignToUsers;


    private PopupForm(String uuid, String descriptor, long startTime, long endTime, boolean isOpenCampaign, List<String> assignees) {
        this.uuid = uuid;
        this.descriptor = descriptor;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isOpenCampaign = isOpenCampaign;
        this.assignToUsers = assignees;
    }

    public static PopupForm fromPopup(Popup existingPopup, PASystem paSystem) {
        String uuid = existingPopup.getUuid();
        List<String> assignees = paSystem.getPopups().getAssignees(uuid);

        return new PopupForm(uuid, existingPopup.getDescriptor(),
                existingPopup.getStartTime(),
                existingPopup.getEndTime(),
                existingPopup.isOpenCampaign(),
                assignees);
    }

    public static PopupForm fromRequest(String uuid, HttpServletRequest request) {
        String descriptor = request.getParameter("descriptor");
        boolean isOpenCampaign = "open-campaign".equals(request.getParameter("open-campaign"));

        long startTime = "".equals(request.getParameter("start_time")) ? 0 : parseTime(request.getParameter("start_time_selected_datetime"));
        long endTime = "".equals(request.getParameter("end_time")) ? 0 : parseTime(request.getParameter("end_time_selected_datetime"));

        List<String> assignees = new ArrayList<String>();
        if (request.getParameter("distribution") != null) {
            for (String user : request.getParameter("distribution").split("[\r\n]+")) {
                if (!user.isEmpty()) {
                    assignees.add(user);
                }
            }
        }

        return new PopupForm(uuid, descriptor, startTime, endTime, isOpenCampaign, assignees);
    }

    public Popup toPopup() {
        return Popup.create(descriptor, startTime, endTime, isOpenCampaign);
    }

}

