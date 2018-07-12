package org.sakaiproject.attendance.services;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.util.ArrayList;
import java.util.List;

public class AttendanceGoogleReportExport {

    private static final String APPLICATION_NAME = "AttendanceGoogleReportExport";

    public AttendanceGoogleReportExport() {
    }

    public void export() {
        try {
            // FIXME need a real spreadsheet id
            final String spreadsheetId = "1D4XcY7fQGfWu3ep_EDKOR-xIAoXPUp3sZyGfLp9ANNs";

            GoogleClient client = new GoogleClient();
            Sheets service = client.getSheets(APPLICATION_NAME);

            List<String> ranges = new ArrayList<>();
            Sheets.Spreadsheets.Get request = service.spreadsheets().get(spreadsheetId);
            request.setRanges(ranges);
            request.setIncludeGridData(false);
            Spreadsheet spreadsheet = request.execute();

            Sheet sheet = spreadsheet.getSheets().get(0);
            String sheetName = sheet.getProperties().getTitle();
            int columns = sheet.getProperties().getGridProperties().getColumnCount();
            int rows = sheet.getProperties().getGridProperties().getRowCount();

            System.out.println("Spreadsheet title: " + sheetName);
            System.out.println("Spreadsheet total columns: " + columns);
            System.out.println("Spreadsheet total rows: " + rows);

            ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, sheetName)
                .execute();

            List<List<Object>> values = response.getValues();

            if (values == null || values.isEmpty()) {
                System.out.println("No data found.");
            } else {
                System.out.println("Testing");
                for (List row : values) {
                    System.out.printf("%s %s %s %s %s\n", row.get(0), row.get(1), row.get(2), row.get(3), row.get(4));
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR in AttendanceGoogleReportExport.export");
            System.out.println(e.getMessage());
        }
    }
}
