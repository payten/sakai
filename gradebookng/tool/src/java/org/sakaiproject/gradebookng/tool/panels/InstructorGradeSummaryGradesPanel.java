package org.sakaiproject.gradebookng.tool.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GbCategoryType;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.util.CourseGradeFormatter;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.tool.component.GbAjaxLink;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.pages.BasePage;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;
import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.tool.gradebook.Gradebook;

public class InstructorGradeSummaryGradesPanel extends Panel {

	private static final long serialVersionUID = 1L;

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;

	GbCategoryType configuredCategoryType;
	boolean isGroupedByCategory = false;
	boolean categoriesEnabled = false;

	public InstructorGradeSummaryGradesPanel(final String id, final IModel<Map<String, Object>> model) {
		super(id, model);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		this.setOutputMarkupId(true);

		// get configured category type
		// TODO this can be fetched from the Gradebook instead
		this.configuredCategoryType = this.businessService.getGradebookCategoryType();

		final Map<String, Object> modelData = (Map<String, Object>) getDefaultModelObject();
		final boolean groupedByCategoryByDefault = (Boolean) modelData.get("groupedByCategoryByDefault");
		this.isGroupedByCategory =  groupedByCategoryByDefault && this.configuredCategoryType != GbCategoryType.NO_CATEGORY;
		this.categoriesEnabled = this.configuredCategoryType != GbCategoryType.NO_CATEGORY;
	}

	@Override
	public void onBeforeRender() {
		super.onBeforeRender();

		final GradebookPage gradebookPage = (GradebookPage) getPage();

		// unpack model
		final Map<String, Object> modelData = (Map<String, Object>) getDefaultModelObject();
		final String userId = (String) modelData.get("userId");

		// get grades and assignments
		final List<Assignment> assignments = this.businessService.getGradebookAssignments();

		// TODO catch if this is null, the get(0) will throw an exception
		// TODO also catch the GbException
		final GbStudentGradeInfo gradeInfo = this.businessService
			.buildGradeMatrix(assignments, Arrays.asList(userId), gradebookPage.getUiSettings()).get(0);
		final List<CategoryDefinition> categories = this.businessService.getGradebookCategories();


		// setup
		final List<String> categoryNames = new ArrayList<String>();
		final Map<String, List<Assignment>> categoryNamesToAssignments = new HashMap<String, List<Assignment>>();

		final boolean[] categoryScoreHidden = { false };

		// iterate over assignments and build map of categoryname to list of assignments
		for (final Assignment assignment : assignments) {

			final String categoryName = getCategoryName(assignment);

			if (!categoryNamesToAssignments.containsKey(categoryName)) {
				categoryNames.add(categoryName);
				categoryNamesToAssignments.put(categoryName, new ArrayList<Assignment>());
			}

			categoryNamesToAssignments.get(categoryName).add(assignment);
		}
		Collections.sort(categoryNames);

		final WebMarkupContainer toggleActions = new WebMarkupContainer("toggleActions");
		toggleActions.setVisible(this.categoriesEnabled);

		final GbAjaxLink toggleCategoriesLink = new GbAjaxLink("toggleCategoriesLink") {
			@Override
			protected void onInitialize() {
				super.onInitialize();
				if (InstructorGradeSummaryGradesPanel.this.isGroupedByCategory) {
					add(new AttributeAppender("class", " on"));
				}
				add(new AttributeModifier("aria-pressed", InstructorGradeSummaryGradesPanel.this.isGroupedByCategory));
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				InstructorGradeSummaryGradesPanel.this.isGroupedByCategory = !InstructorGradeSummaryGradesPanel.this.isGroupedByCategory;

				GradebookUiSettings settings = gradebookPage.getUiSettings();
				settings.setSummaryGroupedByCategory(InstructorGradeSummaryGradesPanel.this.isGroupedByCategory);
				gradebookPage.setUiSettings(settings);

				target.add(InstructorGradeSummaryGradesPanel.this);
				target.appendJavaScript(
					String.format("new GradebookGradeSummary($(\"#%s\"), %s);",
						InstructorGradeSummaryGradesPanel.this.getMarkupId(),
						false));

				if (!InstructorGradeSummaryGradesPanel.this.isGroupedByCategory) {
					// hide the weight column if categories are disabled
					target.appendJavaScript("$('.weight-col').hide();");
				}
			}
		};
		toggleActions.addOrReplace(toggleCategoriesLink);
		toggleActions.addOrReplace(new WebMarkupContainer("expandCategoriesLink").setVisible(isGroupedByCategory));
		toggleActions.addOrReplace(new WebMarkupContainer("collapseCategoriesLink").setVisible(isGroupedByCategory));
		addOrReplace(toggleActions);

		addOrReplace(new WebMarkupContainer("categoryColumnHeader").
			setVisible(this.categoriesEnabled && !this.isGroupedByCategory));

		// output all of the categories
		// within each we then add the assignments in each category
		addOrReplace(new ListView<String>("categoriesList", categoryNames) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<String> categoryItem) {
				final String categoryName = categoryItem.getModelObject();

				final List<Assignment> categoryAssignments = categoryNamesToAssignments.get(categoryName);

				final WebMarkupContainer categoryRow = new WebMarkupContainer("categoryRow");
				categoryRow.setVisible(
					InstructorGradeSummaryGradesPanel.this.categoriesEnabled
						&& InstructorGradeSummaryGradesPanel.this.isGroupedByCategory);
				categoryItem.add(categoryRow);
				categoryRow.add(new Label("category", categoryName));

				if (categoryName.equals(getString(GradebookPage.UNCATEGORISED))) {
					categoryRow.add(new Label("categoryGrade", ""));
					categoryRow.add(new Label("categoryWeight", ""));
				} else {
					CategoryDefinition categoryDefinition = null;
					for (final CategoryDefinition aCategoryDefinition : categories) {
						if (aCategoryDefinition.getName().equals(categoryName)) {
							categoryDefinition = aCategoryDefinition;
							break;
						}
					}

					final Double score = gradeInfo.getCategoryAverages()
						.get(categoryDefinition.getId());
					String grade = "";
					if (score != null) {
						grade = FormatHelper.formatDoubleAsPercentage(score);
					}
					categoryRow.add(new Label("categoryGrade", grade));

					String categoryWeight = "";
					if (!categoryAssignments.isEmpty()) {
						final Double weight = categoryAssignments.get(0).getWeight();
						if (weight != null) {
							categoryWeight = FormatHelper.formatDoubleAsPercentage(weight * 100);
						}
					}
					categoryRow.add(new Label("categoryWeight", categoryWeight));
				}

				categoryItem.add(new ListView<Assignment>("assignmentsForCategory", categoryAssignments) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(final ListItem<Assignment> assignmentItem) {
						final Assignment assignment = assignmentItem.getModelObject();

						if (InstructorGradeSummaryGradesPanel.this.configuredCategoryType == GbCategoryType.NO_CATEGORY) {
							assignmentItem.add(new AttributeAppender("class", " gb-no-categories"));
						}

						final GbGradeInfo grade = gradeInfo.getGrades().get(assignment.getId());

						final String rawGrade;
						String comment;
						if (grade != null) {
							rawGrade = grade.getGrade();
							comment = grade.getGradeComment();
						} else {
							rawGrade = "";
							comment = "";
						}

						final Label title = new Label("title", assignment.getName());
						assignmentItem.add(title);

						final BasePage page = (BasePage) getPage();
						final WebMarkupContainer flags = new WebMarkupContainer("flags");
						flags.add(page.buildFlagWithPopover("isExtraCredit", getString("label.gradeitem.extracredit"))
							.setVisible(assignment.getExtraCredit()));
						flags.add(page.buildFlagWithPopover("isNotCounted", getString("label.gradeitem.notcounted"))
							.setVisible(!assignment.isCounted()));
						flags.add(gradebookPage.buildFlagWithPopover("isNotReleased", getString("label.gradeitem.notreleased"))
							.setVisible(!assignment.isReleased()));
						assignmentItem.add(flags);

						Label dueDate = new Label("dueDate",
							FormatHelper.formatDate(assignment.getDueDate(), getString("label.studentsummary.noduedate")));
						dueDate.add(new AttributeModifier("data-sort-key",
							assignment.getDueDate() == null ? 0 : assignment.getDueDate().getTime()));
						assignmentItem.add(dueDate);
						assignmentItem.add(new Label("grade", FormatHelper.formatGrade(rawGrade)));
						assignmentItem.add(new Label("outOf",
							new StringResourceModel("label.studentsummary.outof", null, new Object[] { assignment.getPoints() })) {
							@Override
							public boolean isVisible() {
								return StringUtils.isNotBlank(rawGrade);
							}
						});
						assignmentItem.add(new Label("comments", comment));
						assignmentItem.add(
							new Label("category", assignment.getCategoryName()).
								setVisible(InstructorGradeSummaryGradesPanel.this.categoriesEnabled
									&& !InstructorGradeSummaryGradesPanel.this.isGroupedByCategory));
					}

					@Override
					public void renderHead(final IHeaderResponse response) {
						super.renderHead(response);

						// hide the weight column if weightings are not enabled
						if (!isCategoryWeightEnabled()) {
							response.render(OnDomReadyHeaderItem.forScript("$('.weight-col').hide();"));
						}

					}
				});
			}
		});

		// no assignments message
		final WebMarkupContainer noAssignments = new WebMarkupContainer("noAssignments") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !assignments.isEmpty();
			}
		};
		addOrReplace(noAssignments);

		// course grade, via the formatter
		final Gradebook gradebook = this.businessService.getGradebook();
		final CourseGrade courseGrade = this.businessService.getCourseGrade(userId);
		final GradebookUiSettings settings = gradebookPage.getUiSettings();

		final CourseGradeFormatter courseGradeFormatter = new CourseGradeFormatter(
			gradebook,
			GbRole.INSTRUCTOR,
			true,
			settings.getShowPoints(),
			true);

		addOrReplace(new Label("courseGrade", courseGradeFormatter.format(courseGrade)).setEscapeModelStrings(false));

		addOrReplace(new Label("courseGradeNotReleasedFlag", "*") {
			@Override
			public boolean isVisible() {
				return !gradebook.isCourseGradeDisplayed();
			}
		});

		addOrReplace(new Label("courseGradeNotReleasedMessage", getString("label.studentsummary.coursegradenotreleasedmessage")) {
			@Override
			public boolean isVisible() {
				return !gradebook.isCourseGradeDisplayed();
			}
		});

		add(new AttributeModifier("data-studentid", userId));

		addOrReplace(new Label("categoryScoreNotReleased", getString("label.studentsummary.categoryscorenotreleased")) {
			@Override
			public boolean isVisible() {
				return categoryScoreHidden[0];
			}
		});
	}

	/**
	 * Helper to get the category name. Looks at settings as well.
	 *
	 * @param assignment
	 * @return
	 */
	private String getCategoryName(final Assignment assignment) {
		if (!this.categoriesEnabled || !this.isGroupedByCategory) {
			return getString(GradebookPage.UNCATEGORISED);
		}
		return StringUtils.isBlank(assignment.getCategoryName()) ? getString(GradebookPage.UNCATEGORISED) : assignment.getCategoryName();
	}

	/**
	 * Helper to determine if weightings are enabled
	 *
	 * @return
	 */
	private boolean isCategoryWeightEnabled() {
		return (this.configuredCategoryType == GbCategoryType.WEIGHTED_CATEGORY) ? true : false;
	}
}
