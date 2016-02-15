package org.sakaiproject.gradebookng.tool.component;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.service.gradebook.shared.GradebookInformation;
import org.sakaiproject.tool.gradebook.Gradebook;

public class GbCourseGradeLabel extends Label {

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;

	private String userId;

	public GbCourseGradeLabel(final String id, final String userId) {
		super(id);
		this.userId = userId;
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		final Gradebook gradebook = this.businessService.getGradebook();
		if (gradebook.isCourseGradeDisplayed()) {

			// check permission for current user to view course grade
			// otherwise fetch and render it
			final String currentUserUuid = this.businessService.getCurrentUser().getId();
			if (!this.businessService.isCourseGradeVisible(currentUserUuid)) {
				setDefaultModel(new ResourceModel("label.coursegrade.nopermission"));
			} else {
				final CourseGrade courseGrade = this.businessService.getCourseGrade(userId);
				final GradebookInformation settings = this.businessService.getGradebookSettings();
				setDefaultModel(Model.of(buildCourseGrade(settings, courseGrade)));
			}
		} else {
			setDefaultModel(Model.of(getString("label.studentsummary.coursegradenotreleased")));
		}
	}

	static public String buildCourseGrade(final GradebookInformation settings, final CourseGrade courseGrade) {
		String result = "";

		// no grade available
		if (StringUtils.isBlank(courseGrade.getEnteredGrade()) && StringUtils.isBlank(courseGrade.getMappedGrade())) {
			return (new ResourceModel("label.studentsummary.coursegrade.none")).getObject();
		}

		if (settings.isCourseLetterGradeDisplayed()) {
			if (StringUtils.isNotBlank(courseGrade.getMappedGrade())) {
				result = courseGrade.getMappedGrade();
			} else if (StringUtils.isNotBlank(courseGrade.getEnteredGrade())) {
				result = courseGrade.getEnteredGrade();
			}
		}

		if (settings.isCourseAverageDisplayed() && StringUtils.isNotBlank(courseGrade.getCalculatedGrade())) {
			String percentage = FormatHelper.formatStringAsPercentage(courseGrade.getCalculatedGrade());
			if (result.isEmpty()) {
				result = percentage;
			} else {
				result += String.format(" (%s)", percentage);
			}
		}

		if (settings.isCoursePointsDisplayed()) {
			// TODO
			String points = "TODO";
			if (result.isEmpty()) {
				result = points;
			} else {
				result += String.format(" [%s]", points);
			}
		}

		return result;
	}
}
