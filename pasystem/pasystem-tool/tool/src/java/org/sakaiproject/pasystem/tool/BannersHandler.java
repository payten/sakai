package org.sakaiproject.pasystem.tool;

import org.sakaiproject.pasystem.api.Banner;
import org.sakaiproject.pasystem.api.PASystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;


public class BannersHandler extends BaseHandler implements Handler {

    private static final Logger LOG = LoggerFactory.getLogger(BannersHandler.class);
    private PASystem paSystem;

    public BannersHandler(PASystem pasystem) {
        this.paSystem = pasystem;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        if (request.getPathInfo().contains("/edit")) {
            if (isGet(request)) {
                handleEdit(extractId(request), context);
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
                handleDelete(extractId(request));
            }
        } else {
            sendRedirect("");
        }
    }


    private void handleDelete(String uuid) {
        paSystem.getBanners().deleteBanner(uuid);

        flash("info", "banner_deleted");
        sendRedirect("");
    }


    private void handleEdit(String uuid, Map<String, Object> context) {
        context.put("subpage", "banner_form");

        Optional<Banner> banner = paSystem.getBanners().getForId(uuid);

        if (banner.isPresent()) {
            showEditForm(BannerForm.fromBanner(banner.get()), context, CrudMode.UPDATE);
        } else {
            LOG.warn("No banner found for UUID: " + uuid);
            sendRedirect("");
        }
    }


    private void showNewForm(Map<String, Object> context) {
        context.put("subpage", "banner_form");
        context.put("mode", "new");
    }

    private void handleCreateOrUpdate(HttpServletRequest request, Map<String, Object> context, CrudMode mode) {
        String uuid = extractId(request);
        BannerForm bannerForm = BannerForm.fromRequest(uuid, request);

        long startTime = bannerForm.getStartTime();
        long endTime = bannerForm.getEndTime();

        if (!bannerForm.hasValidStartTime()) {
            addError("start_time", "invalid_time");
        }

        if (!bannerForm.hasValidEndTime()) {
            addError("end_time", "invalid_time");
        }

        if (!bannerForm.startTimeBeforeEndTime()) {
            addError("start_time", "start_time_after_end_time");
            addError("end_time", "start_time_after_end_time");
        }


        if (hasErrors()) {
            showEditForm(bannerForm, context, mode);
            return;
        }


        if (CrudMode.CREATE.equals(mode)) {
            paSystem.getBanners().createBanner(bannerForm.getMessage(),
                    bannerForm.getHosts(),
                    bannerForm.isDismissible(),
                    bannerForm.isActive(),
                    startTime,
                    endTime);
            flash("info", "banner_created");
        } else {
            paSystem.getBanners().updateBanner(bannerForm.getUuid(),
                    bannerForm.getMessage(),
                    bannerForm.getHosts(),
                    bannerForm.isDismissible(),
                    bannerForm.isActive(),
                    startTime,
                    endTime);
            flash("info", "banner_updated");
        }

        sendRedirect("");
    }


    private void showEditForm(BannerForm bannerForm, Map<String, Object> context, CrudMode mode) {
        context.put("subpage", "banner_form");

        if (CrudMode.UPDATE.equals(mode)) {
            context.put("mode", "edit");
        } else {
            context.put("mode", "new");
        }

        context.put("banner", bannerForm);
    }
}
