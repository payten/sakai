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

public class AttendanceGoogleReportExport {

    private static final String APPLICATION_NAME = "AttendanceGoogleReportExport";

    public AttendanceGoogleReportExport() {
    }

    public void export() {
        try {
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
