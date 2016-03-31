package org.sakaiproject.gradebookng.tool.model;

import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.service.gradebook.shared.Assignment;
import java.util.List;
import org.sakaiproject.gradebookng.business.util.Temp;
import org.sakaiproject.service.gradebook.shared.SortType;
import org.apache.commons.lang.time.StopWatch;

public class AssignmentsAndGrades {
	private List<Assignment> assignments;
	private List<GbStudentGradeInfo> grades;

	public AssignmentsAndGrades(GradebookNgBusinessService businessService,
				    GradebookUiSettings settings) {
		final StopWatch stopwatch = new StopWatch();

		SortType sortBy = SortType.SORT_BY_SORTING;
		if (settings.isCategoriesEnabled()) {
			// Pre-sort assignments by the categorized sort order
			sortBy = SortType.SORT_BY_CATEGORY;
		}

		assignments = businessService.getGradebookAssignments(sortBy);
		Temp.time("getGradebookAssignments", stopwatch.getTime());

		grades = businessService.buildGradeMatrix(assignments,
				settings.getAssignmentSortOrder(), settings.getNameSortOrder(), settings.getCategorySortOrder(),
				settings.getGroupFilter());

		Temp.time("buildGradeMatrix", stopwatch.getTime());
	}

	public List<Assignment> getAssignments() {
		return assignments;
	}

	public List<GbStudentGradeInfo> getGrades() {
		return grades;
	}
}
