package edu.nyu.classes.groupersync.jobs;

import edu.nyu.classes.groupersync.api.UserWithRole;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

class SiteGroupReader {

    private final String siteId;
    private final CourseManagementService cms;

    public SiteGroupReader(String siteId, CourseManagementService cms) {
        this.siteId = siteId;
        this.cms = cms;
    }

    public Collection<SyncableGroup> groups() throws IdUnusedException {
        Collection<SyncableGroup> result = new ArrayList<SyncableGroup>();

        result.addAll(readSakaiGroups());

        return result;
    }

    private Collection<SyncableGroup> readSakaiGroups() throws IdUnusedException {
        Collection<SyncableGroup> result = new ArrayList<SyncableGroup>();

        Collection<AuthzGroup> sakaiGroups = new ArrayList<AuthzGroup>();
        Site site = SiteService.getSite(siteId);
        sakaiGroups.add(site);
        sakaiGroups.addAll(site.getGroups());

        for (AuthzGroup sakaiGroup : sakaiGroups) {
            List<UserWithRole> members = new ArrayList<UserWithRole>();
            Map<String, String> rolesOfActiveUsers = new HashMap<String, String>();
            HashSet<String> inactiveUsers = new HashSet<String>();

            // Load direct members of this group
            for (Member m : sakaiGroup.getMembers()) {
                if (!m.isActive()) {
                    inactiveUsers.add(m.getUserEid());
                    continue;
                }

                // Used below
                rolesOfActiveUsers.put(m.getUserEid(), m.getRole().getId());

                if (!m.isProvided()) {
                    // Provided users will be handled separately below.
                    members.add(new UserWithRole(m.getUserEid(), m.getRole().getId()));
                }
            }

            String provider = sakaiGroup.getProviderGroupId();

            // Plus those provided by sections
            if (provider != null) {
                HashSet<String> seenUsers = new HashSet<String>();

                for (String providerId : provider.split("\\+")) {
                    for (org.sakaiproject.coursemanagement.api.Membership m : cms.getSectionMemberships(providerId)) {
                        if (seenUsers.contains(m.getUserId()) || inactiveUsers.contains(m.getUserId())) {
                            continue;
                        }

                        String userId = m.getUserId();
                        String role = m.getRole();

                        if (rolesOfActiveUsers.containsKey(userId)) {
                            // We'll give priority to the role set manually
                            // within Sakai, just in case the user has had their
                            // SIS role overridden.
                            role = rolesOfActiveUsers.get(userId);
                        }

                        members.add(new UserWithRole(userId, role));
                        seenUsers.add(userId);
                    }
                }
            }

            result.add(new SyncableGroup(sakaiGroup.getId(), sakaiGroup.getDescription(), members));
        }

        return result;
    }
}
