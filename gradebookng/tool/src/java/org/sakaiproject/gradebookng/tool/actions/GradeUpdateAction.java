package org.sakaiproject.gradebookng.tool.actions;


import java.io.Serializable;
import com.fasterxml.jackson.databind.JsonNode;
import java.lang.annotation.Target;
import java.lang.Error;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.DoubleValidator;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.sakaiproject.gradebookng.business.GradeSaveResponse;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.tool.panels.GradeItemCellPanel;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.service.gradebook.shared.GradebookInformation;

public class GradeUpdateAction implements Action, Serializable {

    GradebookNgBusinessService businessService;
    private static final long serialVersionUID = 1L;

    public GradeUpdateAction(GradebookNgBusinessService businessService) {
        this.businessService = businessService;
    }

    // FIXME: We'll use a proper ObjectMapper for these soon.
    private class GradeUpdateResponse implements ActionResponse {
        private String courseGrade;
        private String points;
        private boolean extraCredit;

        public GradeUpdateResponse(String courseGrade, String points, boolean extraCredit) {
            this.courseGrade = courseGrade;
            this.points = points;
        }

        public String getStatus() {
            return "OK";
        }

        public String toJson() {
            return "{\"courseGrade\": [\"" + courseGrade + "\", \"" + points + "\"], \"extraCredit\": " + extraCredit + "}";
        }
    }

    private class ArgumentErrorResponse implements ActionResponse {
        private String msg;
        public ArgumentErrorResponse(String msg) {
            this.msg = msg;
        }

        public String getStatus() {
            return "error";
        }

        public String toJson() {
            return String.format("{\"msg\": \"%s\"}", msg);
        }
    }

    private class SaveGradeErrorResponse implements ActionResponse {
        private GradeSaveResponse serverResponse;
        public SaveGradeErrorResponse(GradeSaveResponse serverResponse) {
            this.serverResponse = serverResponse;
        }

        public String getStatus() {
            return "error";
        }

        public String toJson() {
            // TODO map to a reasonable message based on server response
            return String.format("{\"msg\": \"%s\"}", serverResponse.toString());
        }
    }

    @Override
    public ActionResponse handleEvent(JsonNode params, AjaxRequestTarget target) {
        final String oldGrade = params.get("oldScore").asText();
        final String rawNewGrade = params.get("newScore").asText();

        // perform validation here so we can bypass the backend
        final DoubleValidator validator = new DoubleValidator();

        if (StringUtils.isNotBlank(rawNewGrade) && (!validator.isValid(rawNewGrade) || Double.parseDouble(rawNewGrade) < 0)) {
            return new ArgumentErrorResponse("Grade not valid");
        }

        String assignmentId = params.get("assignmentId").asText();
        String studentUuid = params.get("studentId").asText();

        final String newGrade = FormatHelper.formatGrade(rawNewGrade);
        
        // for concurrency, get the original grade we have in the UI and pass it into the service as a check
        final GradeSaveResponse result = businessService.saveGrade(Long.valueOf(assignmentId), studentUuid,
                oldGrade, newGrade, params.get("comment").asText());

        if (!result.equals(GradeSaveResponse.OK) && !result.equals(GradeSaveResponse.OVER_LIMIT)) {
            return new SaveGradeErrorResponse(result);
        }

        CourseGrade studentCourseGrade = businessService.getCourseGrade(studentUuid);

        if (studentCourseGrade != null) {
            final GradebookInformation settings = businessService.getGradebookSettings();

            String grade = FormatHelper.formatCourseGrade(
                studentCourseGrade,
                settings.isCourseLetterGradeDisplayed(),
                true,
                settings.isCourseAverageDisplayed(),
                settings.isCoursePointsDisplayed());

            String points = FormatHelper.formatDoubleToTwoDecimalPlaces(studentCourseGrade.getPointsEarned());

            return new GradeUpdateResponse(grade, points, result.equals(GradeSaveResponse.OVER_LIMIT));
        } else {
            return new GradeUpdateResponse("-", "0", result.equals(GradeSaveResponse.OVER_LIMIT));
        }


        // TODO here, add the message
        // switch (result) {
        // case OK:
        //     markSuccessful(GradeItemCellPanel.this.gradeCell);
        //     this.originalGrade = newGrade;
        //     refreshCourseGradeAndCategoryAverages(target);
        //     break;
        // case ERROR:
        //     markError(getComponent());
        //     error(getString("message.edititem.error"));
        //     break;
        // case OVER_LIMIT:
        //     markOverLimit(GradeItemCellPanel.this.gradeCell);
        //     refreshCourseGradeAndCategoryAverages(target);
        //     this.originalGrade = newGrade;
        //     break;
        // case NO_CHANGE:
        //     handleNoChange(GradeItemCellPanel.this.gradeCell);
        //     break;
        // case CONCURRENT_EDIT:
        //     mark
        //     error(getString("error.concurrentedit"));
        //     GradeItemCellPanel.this.notifications.add(GradeCellNotification.CONCURRENT_EDIT);
        //     break;
        // default:
        //     throw new UnsupportedOperationException("The response for saving the grade is unknown.");
        // }
    }
}
