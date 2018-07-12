package edu.nyu.classes.servlet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import org.sakaiproject.component.cover.HotReloadConfigurationService;
import org.sakaiproject.component.cover.ServerConfigurationService;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.sakaiproject.db.cover.SqlService;
import java.util.stream.Collectors;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class AttendanceGoogleReportExport {

    private static final String APPLICATION_NAME = "AttendanceGoogleReportExport";

    public AttendanceGoogleReportExport() {
    }

    static class SiteUser {
        public String userid;
        public String netid;
        public String siteid;

        public SiteUser(String userid, String netid, String siteid) {
            if (userid == null || netid == null || siteid == null) {
                throw new IllegalStateException("No nulls");
            }
            this.userid = userid;
            this.netid = netid;
            this.siteid = siteid;
        }

        @Override
        public int hashCode() { return (userid + netid + siteid).hashCode(); }

        @Override
        public boolean equals(Object other) {
            if (this == other) { return true; }
            if (!(other instanceof SiteUser)) { return false; }
            return ((SiteUser)other).userid.equals(userid) && ((SiteUser)other).netid.equals(netid) && ((SiteUser)other).siteid.equals(siteid);
        }
    }

    static class AttendanceEvent {
        public String name;

        public AttendanceEvent(String name) {
            if (name == null) {
                throw new IllegalStateException("No nulls");
            }
            this.name = name;
        }

        @Override
        public int hashCode() { return name.hashCode(); }

        @Override
        public boolean equals(Object other) {
            if (this == other) { return true; }
            if (!(other instanceof AttendanceEvent)) { return false; }
            return ((AttendanceEvent)other).name.equals(name);
        }
    }

    static class UserAtEvent {
        public SiteUser user;
        public AttendanceEvent event;

        public UserAtEvent(SiteUser user, AttendanceEvent event) {
            if (user == null || event == null) {
                throw new IllegalStateException("No nulls");
            }
            this.user = user;
            this.event = event;
        }

        @Override
        public int hashCode() { return user.hashCode() ^ event.hashCode(); }

        @Override
        public boolean equals(Object other) {
            if (this == other) { return true; }
            if (!(other instanceof UserAtEvent)) { return false; }
            return ((UserAtEvent)other).user.equals(user) && ((UserAtEvent)other).event.equals(event);
        }
    }


    private void dumpAllAttendances() throws Exception {
        // Get a list of all students from the sites of interest
        // Get a list of the attendance events for all sites, joined to any attendance records

        Connection conn = SqlService.borrowConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // Get out list of users in sites of interest
            List<SiteUser> users = new ArrayList<>();

            ps = conn.prepareStatement("select ssu.user_id, map.eid, ssu.site_id" +
                                       " from sakai_site_user ssu" +
                                       " inner join sakai_user_id_map map on map.user_id = ssu.user_id" +
                                       " where ssu.permission = 1 AND" +
                                       "   ssu.site_id in (select distinct s.site_id from attendance_site_t s inner join attendance_event_t e on s.a_site_id = e.a_site_id)");

            rs = ps.executeQuery();
            while (rs.next()) {
                users.add(new SiteUser(rs.getString("user_id"), rs.getString("eid"), rs.getString("site_id")));
            }

            rs.close();
            ps.close();

            // Get our list of all events
            List<AttendanceEvent> events = new ArrayList<>();
            ps = conn.prepareStatement("select distinct e.name" +
                                       " from attendance_event_t e");

            rs = ps.executeQuery();

            while (rs.next()) {
                events.add(new AttendanceEvent(rs.getString("name")));
            }

            rs.close();
            ps.close();

            // Get all users at all events
            ps = conn.prepareStatement("select s.site_id, e.name, r.user_id, m.eid, r.status" +
                                       " from attendance_event_t e" +
                                       " inner join attendance_record_t r on e.a_event_id = r.a_event_id" +
                                       " inner join attendance_site_t s on e.a_site_id = s.a_site_id" +
                                       " inner join sakai_user_id_map m on m.user_id = r.user_id");

            rs = ps.executeQuery();

            Map<UserAtEvent, String> statusTable = new HashMap<>();

            // Fill out the values we know
            while (rs.next()) {
                SiteUser user = new SiteUser(rs.getString("user_id"), rs.getString("eid"), rs.getString("site_id"));
                AttendanceEvent event = new AttendanceEvent(rs.getString("name"));

                String status = rs.getString("status");

                if (status != null) {
                    statusTable.put(new UserAtEvent(user, event), status);
                }
            }

            // And fill out any that were missing as UKNOWN
            for (SiteUser user : users) {
                for (AttendanceEvent event : events) {
                    UserAtEvent key = new UserAtEvent(user, event);

                    if (!statusTable.containsKey(key)) {
                        statusTable.put(key, "-");
                    }
                }
            }

            // Print the whole lot!
            System.err.print("\t");
            for (AttendanceEvent event : events) {
                System.err.print(event.name);
                System.err.print("\t");
            }
            System.err.println("");

            for (SiteUser user : users) {
                System.err.print(user.netid);
                System.err.print("\t");
                for (AttendanceEvent event : events) {
                    System.err.print(statusTable.get(new UserAtEvent(user, event)));
                    System.err.print("\t");
                }
                System.err.println("\n");
            }

        } finally {
            if (ps != null) { ps.close(); }
            if (rs != null) { rs.close(); }

            SqlService.returnConnection(conn);
        }

    }

    public void export() {
        try {
            try {
                dumpAllAttendances();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // FIXME need a real spreadsheet id
            final String spreadsheetId = "1D4XcY7fQGfWu3ep_EDKOR-xIAoXPUp3sZyGfLp9ANNs";

            String oauthPropertiesFile = HotReloadConfigurationService.getString("attendance-report.oauth-properties", "attendance_report_oauth_properties_not_set");

            oauthPropertiesFile = ServerConfigurationService.getSakaiHomePath() + "/" + oauthPropertiesFile;

            Properties oauthProperties = new Properties();
            try (FileInputStream fis = new FileInputStream(oauthPropertiesFile)) {
                oauthProperties.load(fis);
            }

            oauthProperties.setProperty("credentials_path", new File(new File(oauthPropertiesFile).getParentFile(),
                                                                     "oauth_credentials").getPath());

            GoogleClient client = new GoogleClient(oauthProperties);
            Sheets service = client.getSheets(APPLICATION_NAME);

            System.out.println("Get the sheet");
            List<String> ranges = new ArrayList<>();
            Sheets.Spreadsheets.Get request = service.spreadsheets().get(spreadsheetId);
            request.setRanges(ranges);
            request.setIncludeGridData(false);
            Spreadsheet spreadsheet = request.execute();

            Sheet sheet = spreadsheet.getSheets().get(0);
            String sheetName = sheet.getProperties().getTitle();
//            String sheetId = sheet.getProperties().getId();
            int columns = sheet.getProperties().getGridProperties().getColumnCount();
            int rowsCount = sheet.getProperties().getGridProperties().getRowCount();

            System.out.println("Spreadsheet title: " + sheetName);
            System.out.println("Spreadsheet total columns: " + columns);
            System.out.println("Spreadsheet total rows: " + rowsCount);
            System.out.println(spreadsheet);

            System.out.println("Protect it");
            AddProtectedRangeRequest addProtectedRangeRequest = new AddProtectedRangeRequest();
            ProtectedRange protectedRange = new ProtectedRange();
            GridRange gridRange = new GridRange();
            gridRange.setSheetId(0);
            protectedRange.setRange(gridRange);
            protectedRange.setEditors(new Editors());
            protectedRange.setRequestingUserCanEdit(true);
            addProtectedRangeRequest.setProtectedRange(protectedRange);

            BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
            List<Request> requests = new ArrayList<>();
            Request wrapperRequest = new Request();
            wrapperRequest.setAddProtectedRange(addProtectedRangeRequest);
            requests.add(wrapperRequest);
            batchUpdateSpreadsheetRequest.setRequests(requests);
            Sheets.Spreadsheets.BatchUpdate batchUpdateRequest =
                service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateSpreadsheetRequest);

            BatchUpdateSpreadsheetResponse batchUpdateSpreadsheetResponse = batchUpdateRequest.execute();
            System.out.println(batchUpdateSpreadsheetResponse);

            System.out.println("Clearing the sheet");
            Sheets.Spreadsheets.Values.Clear clearRequest =
                service.spreadsheets().values().clear(spreadsheetId, sheetName, new ClearValuesRequest());
            ClearValuesResponse clearValuesResponse = clearRequest.execute();
            System.out.println(clearValuesResponse);

            System.out.println("Give it some values");
            ValueRange valueRange = new ValueRange();
            List<List<Object>> rows = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                List<Object> row = new ArrayList<>();
                for (int j = 0; j < 100; j++) {
                    row.add(String.valueOf(i*j*100));
                }
                rows.add(row);
            }

            valueRange.setValues(rows);

            Sheets.Spreadsheets.Values.Update updateRequest =
                service.spreadsheets().values().update(spreadsheetId, sheetName, valueRange);
            updateRequest.setValueInputOption("RAW");
            UpdateValuesResponse updateValuesResponse = updateRequest.execute();
            System.out.println(updateValuesResponse);

            System.out.println("Unprotect it");
            requests = new ArrayList<>();
            Sheets.Spreadsheets.Get getSpreadsheetRequest = service.spreadsheets().get(spreadsheetId);
            Spreadsheet s = getSpreadsheetRequest.execute();
            for (ProtectedRange p : s.getSheets().get(0).getProtectedRanges()) {
                DeleteProtectedRangeRequest deleteProtectedRangeRequest = new DeleteProtectedRangeRequest();
                deleteProtectedRangeRequest.setProtectedRangeId(p.getProtectedRangeId());
                wrapperRequest = new Request();
                wrapperRequest.setDeleteProtectedRange(deleteProtectedRangeRequest);
                requests.add(wrapperRequest);
            }
            batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
            batchUpdateSpreadsheetRequest.setRequests(requests);
            batchUpdateRequest = service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateSpreadsheetRequest);
            batchUpdateSpreadsheetResponse = batchUpdateRequest.execute();
            System.out.println(batchUpdateSpreadsheetResponse);
        } catch (Exception e) {
            System.out.println("ERROR in AttendanceGoogleReportExport.export");
            System.out.println(e.getMessage());
        }
    }
}
