package org.sakaiproject.pasystem.tool.forms;

import java.io.InputStream;
import java.io.IOException;
import lombok.Data;
import org.sakaiproject.pasystem.api.PASystem;
import org.sakaiproject.pasystem.api.Popup;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.sakaiproject.pasystem.tool.handlers.ErrorReporter;
import org.sakaiproject.pasystem.tool.handlers.CrudHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Data
public class PopupForm extends BaseForm {

    private static final Logger LOG = LoggerFactory.getLogger(PopupForm.class);

    private String descriptor;
    private boolean isOpenCampaign;
    private List<String> assignToUsers;
    private Optional<DiskFileItem> templateItem;


    private PopupForm(String uuid,
                      String descriptor,
                      long startTime, long endTime,
                      boolean isOpenCampaign,
                      List<String> assignees,
                      Optional<DiskFileItem> templateItem) {
        this.uuid = uuid;
        this.descriptor = descriptor;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isOpenCampaign = isOpenCampaign;
        this.assignToUsers = assignees;
        this.templateItem = templateItem;
    }


    public static PopupForm fromPopup(Popup existingPopup, PASystem paSystem) {
        String uuid = existingPopup.getUuid();
        List<String> assignees = paSystem.getPopups().getAssignees(uuid);

        return new PopupForm(uuid, existingPopup.getDescriptor(),
                existingPopup.getStartTime(),
                existingPopup.getEndTime(),
                existingPopup.isOpenCampaign(),
                assignees,
                Optional.empty());
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

        Optional<DiskFileItem> templateItem = Optional.empty();
        if (request.getAttribute("template") != null) {
            templateItem = Optional.of((DiskFileItem)request.getAttribute("template"));
        }

        return new PopupForm(uuid, descriptor, startTime, endTime, isOpenCampaign, assignees, templateItem);
    }


    public void validate(ErrorReporter errors, CrudHandler.CrudMode mode) {
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

        if (CrudHandler.CrudMode.CREATE.equals(mode) && !templateItem.isPresent()) {
            errors.addError("template", "template_was_missing");
        }
    }


    public Optional<InputStream> getTemplateStream() {
        try {
            if (templateItem.isPresent() && templateItem.get().getSize() > 0) {
                return Optional.of(templateItem.get().getInputStream());
            }
        } catch (IOException e) {
            LOG.error("IOException while fetching template stream", e);
        }

        return Optional.empty();
    }


    public Popup toPopup() {
        return Popup.create(descriptor, startTime, endTime, isOpenCampaign);
    }
}
