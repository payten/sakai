package org.sakaiproject.pasystem.tool.handlers;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.sakaiproject.pasystem.api.PASystem;
import org.sakaiproject.pasystem.api.Popup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.sakaiproject.pasystem.tool.forms.PopupForm;

public class PopupsHandler extends CrudHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PopupsHandler.class);

    private PASystem paSystem;

    public PopupsHandler(PASystem pasystem) {
        this.paSystem = pasystem;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        if (request.getPathInfo().contains("/preview") && isGet(request)) {
            handlePreview(request, response, context);
        } else {
            super.handle(request, response, context);
        }
    }


    protected void handleEdit(HttpServletRequest request, Map<String, Object> context) {
        String uuid = extractId(request);
        Optional<Popup> popup = paSystem.getPopups().getForId(uuid);

        if (popup.isPresent()) {
            showEditForm(PopupForm.fromPopup(popup.get(), paSystem), context, CrudMode.UPDATE);
        } else {
            flash("danger", "No popup found for UUID: " + uuid);
            sendRedirect("");
        }

    }


    private void handlePreview(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        String uuid = extractId(request);

        context.put("layout", false);
        try {
            String content = paSystem.getPopups().getPopupContent(uuid);

            if (content.isEmpty()) {
                // Don't let the portal buffering hijack our response.
                // Include enough content to count as having returned a
                // body.
                content = "     ";
            }

            response.getWriter().write(content);
        } catch (IOException e) {
            LOG.warn("Write failed while previewing popup", e);
        }
    }


    private void showEditForm(PopupForm popupForm, Map<String, Object> context, CrudMode mode) {
        context.put("subpage", "popup_form");

        if (CrudMode.UPDATE.equals(mode)) {
            context.put("mode", "edit");
        } else {
            context.put("mode", "new");
        }

        context.put("popup", popupForm);
    }


    protected void handleCreateOrUpdate(HttpServletRequest request, Map<String, Object> context, CrudMode mode) {
        String uuid = extractId(request);
        PopupForm popupForm = PopupForm.fromRequest(uuid, request);

        if (!popupForm.hasValidStartTime()) {
            addError("start_time", "invalid_time");
        }

        if (!popupForm.hasValidEndTime()) {
            addError("end_time", "invalid_time");
        }

        if (!popupForm.startTimeBeforeEndTime()) {
            addError("start_time", "start_time_after_end_time");
            addError("end_time", "start_time_after_end_time");
        }

        Optional<InputStream> templateInputStream = fileUploadInputStream(request, "template");

        if (CrudMode.CREATE.equals(mode) && !templateInputStream.isPresent()) {
            addError("template", "template_was_missing");
        }

        if (hasErrors()) {
            showEditForm(popupForm, context, mode);
            return;
        }


        if (CrudMode.CREATE.equals(mode)) {
            paSystem.getPopups().createCampaign(popupForm.toPopup(),
                    templateInputStream.get(),
                    Optional.of(popupForm.getAssignToUsers()));
            flash("info", "popup_created");
        } else {
            paSystem.getPopups().updateCampaign(popupForm.getUuid(),
                    popupForm.toPopup(),
                    templateInputStream,
                    popupForm.isOpenCampaign() ? Optional.empty() : Optional.of(popupForm.getAssignToUsers()));
            flash("info", "popup_updated");
        }

        sendRedirect("");
    }


    private Optional<InputStream> fileUploadInputStream(HttpServletRequest request, String attributeName) {
        if (request.getAttribute(attributeName) != null) {
            DiskFileItem templateItem = (DiskFileItem) request.getAttribute(attributeName);

            if (templateItem.getSize() > 0) {
                try {
                    return Optional.of(templateItem.getInputStream());
                } catch (IOException e) {
                    LOG.warn("Failure while handling template upload", e);
                    addError("template", "template_upload_failed", e.toString());
                }
            }
        }

        return Optional.empty();
    }


    protected void showNewForm(Map<String, Object> context) {
        context.put("subpage", "popup_form");
        context.put("mode", "new");
        context.put("templateRequired", true);
    }


    protected void handleDelete(HttpServletRequest request) {
        String uuid = extractId(request);
        paSystem.getPopups().deleteCampaign(uuid);

        flash("info", "popup_deleted");
        sendRedirect("");
    }
}
