package org.sakaiproject.pasystem.impl.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.json.simple.JSONObject;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProviderManager;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.pasystem.api.PASystem;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.tool.cover.SessionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PASystemEntityProvider implements EntityProvider, AutoRegisterEntityProvider, ActionsExecutable, Outputable, Describeable {

    private static final Logger LOG = LoggerFactory.getLogger(PASystemEntityProvider.class);


    @Override
    public String[] getHandledOutputFormats() {
        return new String[] { Formats.JSON };
    }

    @Override
    public String getEntityPrefix() {
        return "pasystem";
    }

    @EntityCustomAction(action = "popupAcknowledge", viewKey = EntityView.VIEW_NEW)
    public String popupAcknowledge(EntityView view, Map<String, Object> params) {
        JSONObject result = new JSONObject();

        result.put("status", "ERROR");

        Object sessionToken = SessionManager.getCurrentSession().getAttribute("sakai.csrf.token");

        if (sessionToken == null || !((String)sessionToken).equals(params.get("sakai_csrf_token"))) {
            LOG.warn("CSRF token validation failed");
            return result.toJSONString();
        }

        User currentUser = UserDirectoryService.getCurrentUser();
        String uuid = (String)params.get("uuid");
        String acknowledgement = (String)params.get("acknowledgement");
        String eid = currentUser.getEid();

        if (uuid == null || acknowledgement == null || eid == null) {
            LOG.warn("Parameter mismatch: {}", params);
            return result.toJSONString();
        }
            

        PASystem paSystem = (PASystem) ComponentManager.get(PASystem.class);
        paSystem.getPopups().acknowledge(uuid, eid, acknowledgement);
        result.put("status", "SUCCESS");

        return result.toJSONString();
    }


    @EntityCustomAction(action = "checkTimeZone", viewKey = EntityView.VIEW_LIST)
    public String checkTimeZone(EntityView view, Map<String, Object> params) {
        TimezoneChecker checker = new TimezoneChecker();
        JSONObject result = new JSONObject();

        result.put("status", "OK");

        String timezoneFromUser = (String)params.get("timezone");

        if (timezoneFromUser != null && checker.timezoneMismatch(timezoneFromUser)) {
            result.put("status", "MISMATCH");
            result.put("setTimezoneUrl", checker.getTimezoneToolUrlForUser());
            result.put("prefsTimezone", checker.formatTimezoneFromProfile());
            result.put("reportedTimezone", checker.formatReportedTimezone(timezoneFromUser));
        }

        return result.toJSONString();
    }



    class TimezoneChecker {

        public String getTimezoneToolUrlForUser()
        {
            User thisUser = UserDirectoryService.getCurrentUser();
            String userid = thisUser.getId();

            try {
                Site userSite = SiteService.getSite("~"+userid);
                ToolConfiguration preferences = userSite.getToolForCommonId("sakai.preferences");
                return String.format("/portal/site/~%s/tool/%s/timezone", userid, preferences.getId());
            } catch (Exception ex) {
                return null;
            }
        }


        public boolean timezoneMismatch(String timezoneFromUser) {
            TimeZone preferredTimeZone = TimeService.getLocalTimeZone();
            TimeZone reportedTimeZone = TimeZone.getTimeZone(timezoneFromUser);

            long now = new Date().getTime();

            return (preferredTimeZone.getOffset(now) != reportedTimeZone.getOffset(now));
        }


        public String formatTimezoneFromProfile() {
            return formatTimezone(TimeService.getLocalTimeZone());
        }


        public String formatReportedTimezone(String timezoneFromUser) {
            return formatTimezone(TimeZone.getTimeZone(timezoneFromUser));
        }


        public String formatTimezone(TimeZone tz) {
            return tz.getID() + " " + formatOffset(tz);
        }


        private String formatOffset(TimeZone tz)
        {
            long now = new Date().getTime();

            long offset = tz.getOffset(now);

            int mins = 60 * 1000;
            int hour = 60 * mins;
            return "(GMT " + String.format("%s%0,2d:%0,2d",
                                           ((offset >= 0) ? "+" : ""),
                                           (offset / hour),
                                           ((offset % hour) / mins)) + ")";
        }

    }


    private EntityProviderManager entityProviderManager;

    public void setEntityProviderManager(EntityProviderManager entityProviderManager) {
        this.entityProviderManager = entityProviderManager;
    }

   protected DeveloperHelperService developerHelperService;

   public void setDeveloperHelperService(DeveloperHelperService developerHelperService) {
      this.developerHelperService = developerHelperService;
   }
}
