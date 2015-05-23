package org.sakaiproject.pasystem.tool;

import lombok.Data;
import java.text.ParseException;
import java.text.SimpleDateFormat;


@Data
class BaseForm {

    protected String uuid;
    protected long startTime;
    protected long endTime;


    protected static long parseTime(String timeString) {
        if (timeString == null || "".equals(timeString)) {
            return 0;
        }

        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(timeString).getTime();
        } catch (ParseException e) {
            return -1;
        }
    }


    public boolean hasValidStartTime() {
        return startTime >= 0;
    }


    public boolean hasValidEndTime() {
        return endTime >= 0;
    }


    public boolean startTimeBeforeEndTime() {
        if (startTime <= 0 || endTime <= 0) {
            return true;
        } else {
            return startTime <= endTime;
        }
    }
}

