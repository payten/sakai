package org.sakaiproject.gradebookng.tool.model;

import java.util.List;

import org.apache.commons.lang.time.StopWatch;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.util.Temp;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;
import org.sakaiproject.service.gradebook.shared.GradebookInformation;
import org.sakaiproject.service.gradebook.shared.SortType;

public class GbGradeTableData {
    private List<Assignment> assignments;
    private List<GbStudentGradeInfo> grades;
    private List<CategoryDefinition> categories;
    private GradebookInformation gradebookInformation;

    public GbGradeTableData(GradebookNgBusinessService businessService,
                            GradebookUiSettings settings) {
        final StopWatch stopwatch = new StopWatch();

        SortType sortBy = SortType.SORT_BY_SORTING;
        if (settings.isCategoriesEnabled()) {
            // Pre-sort assignments by the categorized sort order
            sortBy = SortType.SORT_BY_CATEGORY;
        }

        assignments = businessService.getGradebookAssignments(sortBy);
        Temp.time("getGradebookAssignments", stopwatch.getTime());

        grades = businessService.buildGradeMatrix(
            assignments,
            settings);

        Temp.time("buildGradeMatrix", stopwatch.getTime());

        categories = businessService.getGradebookCategories();
        Temp.time("getGradebookCategories", stopwatch.getTime());

        gradebookInformation = businessService.getGradebookSettings();
        Temp.time("getGradebookSettings", stopwatch.getTime());
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public List<GbStudentGradeInfo> getGrades() {
        return grades;
    }

    public List<CategoryDefinition> getCategories() {
        return categories;
    }

    public GradebookInformation getGradebookInformation() {
        return gradebookInformation;
    }
}