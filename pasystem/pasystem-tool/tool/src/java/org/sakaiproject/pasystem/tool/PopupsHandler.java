package org.sakaiproject.pasystem.tool;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.sakaiproject.pasystem.api.PASystem;
import org.sakaiproject.pasystem.api.Popup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PopupsHandler extends BaseHandler implements Handler {

    private static final Logger LOG = LoggerFactory.getLogger(PopupsHandler.class);

    private PASystem paSystem;

    public PopupsHandler(PASystem pasystem) {
        this.paSystem = pasystem;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        if (request.getPathInfo().contains("/edit")) {
            if (isGet(request)) {
                String uuid = extractId(request);
                Optional<Popup> popup = paSystem.getPopups().getForId(uuid);

                if (popup.isPresent()) {
                    showEditForm(PopupForm.fromPopup(popup.get(), paSystem), context, CrudMode.UPDATE);
                } else {
                    flash("danger", "No popup found for UUID: " + uuid);
                    sendRedirect("");
                }
            } else if (isPost(request)) {
                handleCreateOrUpdate(request, context, CrudMode.UPDATE);
            }
        } else if (request.getPathInfo().contains("/new")) {
            if (isGet(request)) {
                showNewForm(context);
            } else if (isPost(request)) {
                handleCreateOrUpdate(request, context, CrudMode.CREATE);
            }
        } else if (request.getPathInfo().contains("/delete")) {
            if (isGet(request)) {
                sendRedirect("");
            } else if (isPost(request)) {
                handleDelete(extractId(request), context);
            }
        } else if (request.getPathInfo().contains("/preview")) {
            if (isGet(request)) {
                String uuid = extractId(request);

                context.put("layout", false);
                try {
                    String content = paSystem.getPopups().getPopupContent(uuid);

                    if (content.isEmpty()) {
                        // Don't let the portal buffering hijack our response.
                        content = "     ";
                    }

                    response.getWriter().write(content);
                } catch (IOException e) {
                    LOG.warn("Write failed while previewing popup", e);
                }
            }
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


    private void handleCreateOrUpdate(HttpServletRequest request, Map<String, Object> context, CrudMode mode) {
        String uuid = extractId(request);
        PopupForm popupForm = PopupForm.fromRequest(uuid, request);

        if (!popupForm.hasValidStartTime()) {
            add_error("start_time", "invalid_time");
        }

        if (!popupForm.hasValidEndTime()) {
            add_error("end_time", "invalid_time");
        }

        if (!popupForm.startTimeBeforeEndTime()) {
            add_error("start_time", "start_time_after_end_time");
            add_error("end_time", "start_time_after_end_time");
        }

        Optional<InputStream> templateInputStream = fileUploadInputStream(request, "template");

        if (CrudMode.CREATE.equals(mode) && !templateInputStream.isPresent()) {
            add_error("template", "template_was_missing");
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
                    add_error("template", "template_upload_failed", e.toString());
                }
            }
        }

        return Optional.empty();
    }


    private void showNewForm(Map<String, Object> context) {
        context.put("subpage", "popup_form");
        context.put("mode", "new");
        context.put("templateRequired", true);
    }


    private void handleDelete(String uuid, Map<String, Object> context) {
        paSystem.getPopups().deleteCampaign(uuid);

        flash("info", "popup_deleted");
        sendRedirect("");
    }
}
