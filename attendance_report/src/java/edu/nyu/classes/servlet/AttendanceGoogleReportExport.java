package edu.nyu.classes.servlet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import org.sakaiproject.component.cover.HotReloadConfigurationService;
import org.sakaiproject.component.cover.ServerConfigurationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.sakaiproject.db.cover.SqlService;
import java.util.stream.Collectors;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

public class AttendanceGoogleReportExport {

    private static final String APPLICATION_NAME = "AttendanceGoogleReportExport";

    private String spreadsheetId;
    private GoogleClient client;
    private Sheets service;

    public AttendanceGoogleReportExport() {
        String oauthPropertiesFile = HotReloadConfigurationService.getString("attendance-report.oauth-properties", "attendance_report_oauth_properties_not_set");
        oauthPropertiesFile = ServerConfigurationService.getSakaiHomePath() + "/" + oauthPropertiesFile;

        try {
            Properties oauthProperties = new Properties();
            try (FileInputStream fis = new FileInputStream(oauthPropertiesFile)) {
                oauthProperties.load(fis);
            }

            oauthProperties.setProperty("credentials_path", new File(new File(oauthPropertiesFile).getParentFile(),
                "oauth_credentials").getPath());

            this.client = new GoogleClient(oauthProperties);
            this.service = client.getSheets(APPLICATION_NAME);

            // FIXME from config
            this.spreadsheetId = "1D4XcY7fQGfWu3ep_EDKOR-xIAoXPUp3sZyGfLp9ANNs";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    abstract static class ValueObject {
        public abstract Object[] interestingFields();

        @Override
        public int hashCode() { return Arrays.hashCode(interestingFields()); }

        @Override
        public boolean equals(Object other) {
            if (this == other) { return true; }
            if (this.getClass() != other.getClass()) { return false; }
            return Arrays.equals(this.interestingFields(), ((ValueObject) other).interestingFields());
        }
    }

    static class SiteUser extends ValueObject {
        public String userid;
        public String netid;
        public String siteid;

        public SiteUser(String userid, String netid, String siteid) {
            this.userid = Objects.requireNonNull(userid);
            this.netid = Objects.requireNonNull(netid);
            this.siteid = Objects.requireNonNull(siteid);
        }

        @Override
        public Object[] interestingFields() {
            return new Object[] { userid, netid, siteid };
        }
    }

    static class AttendanceEvent extends ValueObject {
        public String name;

        public AttendanceEvent(String name) {
            this.name = Objects.requireNonNull(name);
        }

        @Override
        public Object[] interestingFields() {
            return new Object[] { name };
        }
    }

    static class UserAtEvent extends ValueObject {
        public SiteUser user;
        public AttendanceEvent event;

        public UserAtEvent(SiteUser user, AttendanceEvent event) {
            this.user = Objects.requireNonNull(user);
            this.event = Objects.requireNonNull(event);
        }

        @Override
        public Object[] interestingFields() {
            return new Object[] { user, event };
        }
    }

    static class DataTable {
        public Set<AttendanceEvent> events;
        public List<SiteUser> users;
        public Map<UserAtEvent, String> statusTable;

        public DataTable(List<SiteUser> users, Set<AttendanceEvent> events, Map<UserAtEvent, String> statusTable) {
            this.users = users;
            this.events = events;
            this.statusTable = statusTable;
        }
    }


    private DataTable loadAllData() throws Exception {
        // Get a list of all students from the sites of interest
        // Get a list of the attendance events for all sites, joined to any attendance records

        Connection conn = SqlService.borrowConnection();
        try {
            // Get out list of users in sites of interest
            List<SiteUser> users = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement("select ssu.user_id, map.eid, ssu.site_id" +
                                                              " from sakai_site_user ssu" +
                                                              " inner join sakai_user_id_map map on map.user_id = ssu.user_id" +
                                                              " where ssu.permission = 1 AND" +
                                                              "   ssu.site_id in (select distinct s.site_id from attendance_site_t s inner join attendance_event_t e on s.a_site_id = e.a_site_id)");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new SiteUser(rs.getString("user_id"), rs.getString("eid"), rs.getString("site_id")));
                }
            }


            // Get our mapping of events to the sites that have them
            Map<AttendanceEvent, Set<String>> sitesWithEvent = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement("select e.name, s.site_id" +
                                                              " from attendance_event_t e" +
                                                              " inner join attendance_site_t s on s.a_site_id = e.a_site_id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceEvent event = new AttendanceEvent(rs.getString("name"));

                    if (!sitesWithEvent.containsKey(event)) {
                        sitesWithEvent.put(event, new HashSet<>());
                    }

                    sitesWithEvent.get(event).add(rs.getString("site_id"));
                }

            }

            Set<AttendanceEvent> events = sitesWithEvent.keySet();
            Map<UserAtEvent, String> statusTable = new HashMap<>();

            // If a user is in a site that doesn't have a particular event, that's a "-"
            for (SiteUser user : users) {
                for (AttendanceEvent event : events) {
                    if (!sitesWithEvent.get(event).contains(user.siteid)) {
                        statusTable.put(new UserAtEvent(user, event), "-");
                    }
                }
            }

            // Get all users at all events
            try (PreparedStatement ps = conn.prepareStatement("select s.site_id, e.name, r.user_id, m.eid, r.status" +
                                                              " from attendance_event_t e" +
                                                              " inner join attendance_record_t r on e.a_event_id = r.a_event_id" +
                                                              " inner join attendance_site_t s on e.a_site_id = s.a_site_id" +
                                                              " inner join sakai_user_id_map m on m.user_id = r.user_id");
                 ResultSet rs = ps.executeQuery()) {
                // Fill out the values we know
                while (rs.next()) {
                    SiteUser user = new SiteUser(rs.getString("user_id"), rs.getString("eid"), rs.getString("site_id"));
                    AttendanceEvent event = new AttendanceEvent(rs.getString("name"));

                    String status = rs.getString("status");

                    if (status != null) {
                        statusTable.put(new UserAtEvent(user, event), status);
                    }
                }
            }

            // And fill out any that were missing as UKNOWN
            for (SiteUser user : users) {
                for (AttendanceEvent event : events) {
                    UserAtEvent key = new UserAtEvent(user, event);

                    if (!statusTable.containsKey(key)) {
                        statusTable.put(key, "UNKNOWN");
                    }
                }
            }

            return new DataTable(users, events, statusTable);

        } finally {
            SqlService.returnConnection(conn);
        }
    }

    public void export() {
        try {
            Sheet sheet = getTargetSheet();

            protectSheet(sheet);
            clearSheet(sheet);
            syncValuesToSheet(sheet);
            clearProtectedRanges(sheet);
        } catch (Exception e) {
            System.out.println("ERROR in AttendanceGoogleReportExport.export");
            System.out.println(e.getMessage());
        }
    }

    private ProtectedRange protectSheet(Sheet sheet) throws IOException {
        System.out.println("Protect sheet");
        AddProtectedRangeRequest addProtectedRangeRequest = new AddProtectedRangeRequest();
        ProtectedRange protectedRange = new ProtectedRange();
        GridRange gridRange = new GridRange();
        gridRange.setSheetId(sheet.getProperties().getSheetId());
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
        AddProtectedRangeResponse addProtectedRangeResponse = batchUpdateSpreadsheetResponse.getReplies().get(0).getAddProtectedRange();

        return addProtectedRangeResponse.getProtectedRange();
    }

    private void clearProtectedRanges(Sheet sheet) throws IOException {
        System.out.println("Clear all protected ranges");
        List<Request> requests = new ArrayList<>();
        Sheets.Spreadsheets.Get getSpreadsheetRequest = service.spreadsheets().get(spreadsheetId);
        Spreadsheet spreadsheet = getSpreadsheetRequest.execute();

        Sheet sheetToClear = null;
        for (Sheet freshSheet : spreadsheet.getSheets()) {
            if (freshSheet.getProperties().getSheetId() == sheet.getProperties().getSheetId()) {
                sheetToClear = freshSheet;
                break;
            }
        }
        if (sheetToClear == null) {
            throw new RuntimeException("Sheet not found");
        }

        for (ProtectedRange protectedRange : sheetToClear.getProtectedRanges()) {
            DeleteProtectedRangeRequest deleteProtectedRangeRequest = new DeleteProtectedRangeRequest();
            deleteProtectedRangeRequest.setProtectedRangeId(protectedRange.getProtectedRangeId());
            Request request = new Request();
            request.setDeleteProtectedRange(deleteProtectedRangeRequest);
            requests.add(request);
        }
        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
        batchUpdateSpreadsheetRequest.setRequests(requests);
        Sheets.Spreadsheets.BatchUpdate batchUpdateRequest = service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateSpreadsheetRequest);
        BatchUpdateSpreadsheetResponse batchUpdateSpreadsheetResponse = batchUpdateRequest.execute();

        System.out.println(batchUpdateSpreadsheetResponse);
    }

    private void clearSheet(Sheet sheet) throws IOException {
        System.out.println("Clear the sheet");
        Sheets.Spreadsheets.Values.Clear clearRequest =
            service.spreadsheets().values().clear(spreadsheetId, sheet.getProperties().getTitle(), new ClearValuesRequest());
        ClearValuesResponse clearValuesResponse = clearRequest.execute();
        System.out.println(clearValuesResponse);
    }

    private void syncValuesToSheet(Sheet sheet) throws Exception {
        System.out.println("Give it some values");
        ValueRange valueRange = new ValueRange();
        List<List<Object>> rows = new ArrayList<>();

        DataTable table = loadAllData();

        // Add a row for our header
        List<Object> header = new ArrayList<>();
        header.add("");
        header.add("");
        for (AttendanceEvent event : table.events) {
            header.add(event.name);
            header.add(event.name + " [override]");
        }
        rows.add(header);

        // Now our student data
        for (SiteUser user : table.users) {
            List<Object> row = new ArrayList<>();
            row.add(user.netid);
            row.add(user.siteid);

            for (AttendanceEvent event : table.events) {
                row.add(table.statusTable.get(new UserAtEvent(user, event)));
                row.add("");
            }

            rows.add(row);
        }

        valueRange.setValues(rows);

        Sheets.Spreadsheets.Values.Update updateRequest =
            service.spreadsheets().values().update(spreadsheetId, sheet.getProperties().getTitle(), valueRange);
        updateRequest.setValueInputOption("RAW");
        UpdateValuesResponse updateValuesResponse = updateRequest.execute();
        System.out.println(updateValuesResponse);
    }

    private Sheet getTargetSheet() throws IOException {
        System.out.println("Get the sheet");
        List<String> ranges = new ArrayList<>();
        Sheets.Spreadsheets.Get request = service.spreadsheets().get(spreadsheetId);
        request.setRanges(ranges);
        request.setIncludeGridData(false);
        Spreadsheet spreadsheet = request.execute();

        return spreadsheet.getSheets().get(0);
    }
}
