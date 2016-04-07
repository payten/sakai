package org.sakaiproject.gradebookng.tool.model;

import lombok.Value;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;

import org.apache.wicket.Component;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.business.model.GbGradeInfo;
import java.io.UnsupportedEncodingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;
import org.sakaiproject.service.gradebook.shared.GradebookInformation;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class GbGradebookData {

    private int NULL_SENTINEL = 127;

    @Value
    private class StudentDefinition {
        private String eid;
        private String userId;
        private String firstName;
        private String lastName;

        private String hasComments;
    }

    private interface ColumnDefinition {
        public String getType();
        public String getValueFor(GbStudentGradeInfo studentGradeInfo);
    }

    @Value
    private class AssignmentDefinition implements ColumnDefinition {
        private Long assignmentId;
        private String title;
        private String points;
        private String dueDate;

        private boolean isReleased;
        private boolean isIncludedInCourseGrade;
        private boolean isExtraCredit;
        private boolean isExternallyMaintained;
        private String externalId;
        private String externalAppName;

        private String categoryId;
        private String categoryName;
        private String categoryColor;
        private String categoryWeight;
        private boolean isCategoryExtraCredit;

        @Override
        public String getType() {
            return "assignment";
        }

        @Override
        public String getValueFor(GbStudentGradeInfo studentGradeInfo) {
            Map<Long, GbGradeInfo> studentGrades = studentGradeInfo.getGrades();

            GbGradeInfo gradeInfo = studentGrades.get(assignmentId);

            if (gradeInfo == null) {
                return null;
            } else {
                String grade = gradeInfo.getGrade();
                return grade;
            }
        }
    }

    @Value
    private class CategoryAverageDefinition implements ColumnDefinition {
        private Long categoryId;
        private String title;
        private String color;

        @Override
        public String getType() {
            return "category";
        }

        @Override
        public String getValueFor(GbStudentGradeInfo studentGradeInfo) {
            Map<Long, Double> categoryAverages = studentGradeInfo.getCategoryAverages();

            Double average = categoryAverages.get(categoryId);

            if (average == null) {
                return null;
            } else {
		final NumberFormat df = NumberFormat.getInstance();
		df.setMinimumFractionDigits(0);
		df.setMaximumFractionDigits(2);
		df.setGroupingUsed(false);
		df.setRoundingMode(RoundingMode.HALF_DOWN);

		return df.format(average);
            }
        }
    }

    @Value
    private class DataSet {
        private List<StudentDefinition> students;
        private List<ColumnDefinition> columns;
        private List<String> courseGrades;
        private String serializedGrades;

        private int rowCount;
        private int columnCount;

        public DataSet(List<StudentDefinition> students,
                       List<ColumnDefinition> columns,
                       List<String> courseGrades,
                       String serializedGrades) {
            this.students = students;
            this.columns = columns;
            this.courseGrades = courseGrades;
            this.serializedGrades = serializedGrades;

            this.rowCount = students.size();
            this.columnCount = columns.size();
        }
    }

    private List<StudentDefinition> students;
    private List<ColumnDefinition> columns;
    private List<GbStudentGradeInfo> studentGradeInfoList;
    private List<CategoryDefinition> categories;
    private GradebookInformation settings;

    private Component parent;

    public GbGradebookData(List<GbStudentGradeInfo> studentGradeInfoList,
                           List<Assignment> assignments,
                           List<CategoryDefinition> categories,
                           GradebookInformation settings,
                           Component parentComponent) {
        this.parent = parentComponent;
        this.categories = categories;
        this.settings = settings;

        this.studentGradeInfoList = studentGradeInfoList;

        this.columns = loadColumns(assignments);
        this.students = loadStudents(studentGradeInfoList);
    }

    public String toScript() {
        ObjectMapper mapper = new ObjectMapper();

        DataSet dataset = new DataSet(this.students, this.columns, courseGrades(), serializeGrades(gradeList()));

        try {
            return mapper.writeValueAsString(dataset);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String serializeGrades(List<String> gradeList) {
        StringBuilder sb = new StringBuilder();

        for (String gradeString : gradeList) {
            if (gradeString.isEmpty()) {
                // No grade set.  Use a sentinel value.
                sb.appendCodePoint(NULL_SENTINEL);
                continue;
            }

            double grade = Double.valueOf(gradeString);

            boolean hasFraction = ((int)grade != grade);

            if (grade < 127 && !hasFraction) {
                // single byte, no fraction
                sb.appendCodePoint((int)grade & 0xFF);
            } else if (grade < 16384 && !hasFraction) {
                // two byte, no fraction
                sb.appendCodePoint(((int)grade >> 8) | 128);
                sb.appendCodePoint(((int)grade & 0xFF));
            } else if (grade < 16384) {
                // three byte encoding, fraction
                sb.appendCodePoint(((int)grade >> 8) | 192);
                sb.appendCodePoint((int)grade & 0xFF);
                sb.appendCodePoint(decimalToInteger((grade - (int)grade),
                        2));
            } else {
                throw new RuntimeException("Grade too large: " + grade);
            }
        }

        try {
            return Base64.getEncoder().encodeToString(sb.toString().getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private int decimalToInteger(double decimal, int places) {
        if ((int)decimal == decimal) {
            return (int)decimal;
        } else if (places == 0) {
            if ((decimal - (int)decimal) >= 0.5) {
                return (int)decimal + 1;
            } else {
                return (int)decimal;
            }
        } else {
            return decimalToInteger(decimal * 10, places - 1);
        }
    }

    private List<String> courseGrades() {
        List<String> result = new ArrayList<String>();

        // FIXME: Reuse logic from existing code to get this.  Need to format
        // the thing appropriately for the type of course grade.
        for (GbStudentGradeInfo studentGradeInfo : this.studentGradeInfoList) {
            result.add(studentGradeInfo.getCourseGrade().getMappedGrade());
        }

        return result;
    }


    private List<String> gradeList() {
        List<String> result = new ArrayList<String>();

        for (GbStudentGradeInfo studentGradeInfo : this.studentGradeInfoList) {
            for (ColumnDefinition column : this.columns) {
                String grade = column.getValueFor(studentGradeInfo);

                if (grade == null) {
                    result.add("");
                } else {
                    result.add(grade);
                }
            }
        }

        return result;

    }

    private String getString(String key) {
        return parent.getString(key);
    }

    private List<StudentDefinition> loadStudents(List<GbStudentGradeInfo> studentInfo) {
        List<StudentDefinition> result = new ArrayList<StudentDefinition>();

        for (GbStudentGradeInfo student : studentInfo) {
            result.add(new StudentDefinition(student.getStudentEid(),
                    student.getStudentUuid(),
                    student.getStudentFirstName(),
                    student.getStudentLastName(),
                    formatCommentData(student)));
        }

        return result;
    }

    private List<ColumnDefinition> loadColumns(List<Assignment> assignments) {
        GradebookUiSettings userSettings = ((GradebookPage) parent.getPage()).getUiSettings();

        List<ColumnDefinition> result = new ArrayList<ColumnDefinition>();

        if (assignments.isEmpty()) {
            return result;
        }

        for (int i = 0; i < assignments.size(); i++) {
            Assignment a1 = assignments.get(i);
            Assignment a2 = ((i + 1) < assignments.size()) ? assignments.get(i + 1) : null;

            result.add(new AssignmentDefinition(a1.getId(),
                                                a1.getName(),
                                                a1.getPoints().toString(),
                                                FormatHelper.formatDate(a1.getDueDate(), getString("label.studentsummary.noduedate")),

                                                a1.isReleased(),
                                                a1.isCounted(),
                                                a1.isExtraCredit(),
                                                a1.isExternallyMaintained(),
                                                a1.getExternalId(),
                                                a1.getExternalAppName(),

                                                nullable(a1.getCategoryId()),
                                                a1.getCategoryName(),
                                                userSettings.getCategoryColor(a1.getCategoryName()),
                                                nullable(a1.getWeight()),
                                                a1.isCategoryExtraCredit()));


            // If we're at the end of the assignment list, or we've just changed
            // categories, put out a total.
            if (userSettings.isCategoriesEnabled() &&
                a1.getCategoryId() != null &&
                (a2 == null || !a1.getCategoryId().equals(a2.getCategoryId()))) {
                result.add(new CategoryAverageDefinition(a1.getCategoryId(),
                                                         a1.getCategoryName(),
                                                         userSettings.getCategoryColor(a1.getCategoryName())));
            }
        }

        // if group by categories is disabled, then show all catagory scores
        // at the end of the table
        if (!userSettings.isCategoriesEnabled()) {
            for (CategoryDefinition category : categories) {
                if (!category.getAssignmentList().isEmpty()) {
                    result.add(new CategoryAverageDefinition(
                        category.getId(),
                        category.getName(),
                        userSettings.getCategoryColor(category.getName())));
                }
            }
        }

        return result;
    }

    private String formatCommentData(GbStudentGradeInfo student) {
        StringBuilder sb = new StringBuilder();

        for (ColumnDefinition column : this.columns) {
            if (column instanceof AssignmentDefinition) {
                AssignmentDefinition assignmentColumn = (AssignmentDefinition) column;
                GbGradeInfo gradeInfo = student.getGrades().get(assignmentColumn.getAssignmentId());
                if (gradeInfo != null && !StringUtils.isBlank(gradeInfo.getGradeComment())) {
                    sb.append('1');
                } else {
                    sb.append('0');
                }
            } else {
                sb.append('0');
            }
        }

        return sb.toString();
    }

    private String nullable(Object value) {
        if (value == null) {
            return null;
        } else {
            return value.toString();
        }
    }

}
