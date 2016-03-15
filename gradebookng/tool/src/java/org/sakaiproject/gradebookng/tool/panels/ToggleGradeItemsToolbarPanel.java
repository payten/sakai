package org.sakaiproject.gradebookng.tool.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.service.gradebook.shared.Assignment;

/**
 * Panel that renders the list of assignments and categories and allows the user to toggle each one on and off from the display.
 */
public class ToggleGradeItemsToolbarPanel extends Panel {

	private static final long serialVersionUID = 1L;

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;

	public ToggleGradeItemsToolbarPanel(final String id, final IModel<Map<String, Object>> model) {
		super(id, model);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		// setup
		final List<String> categoryNames = new ArrayList<String>();
		final Map<String, List<Assignment>> categoryNamesToAssignments = new HashMap<String, List<Assignment>>();

		final Map<String, Object> model = (Map<String, Object>) getDefaultModelObject();
		final List<Assignment> assignments = (List<Assignment>) model.get("assignments");
		final GradebookUiSettings settings = (GradebookUiSettings) model.get("settings");
		final boolean categoriesEnabled = (Boolean) model.get("categoriesEnabled");

		// iterate over assignments and build map of categoryname to list of assignments
		for (final Assignment assignment : assignments) {

			final String categoryName = getCategoryName(assignment, categoriesEnabled);

			if (!categoryNamesToAssignments.containsKey(categoryName)) {
				categoryNames.add(categoryName);
				categoryNamesToAssignments.put(categoryName, new ArrayList<Assignment>());
			}

			categoryNamesToAssignments.get(categoryName).add(assignment);
		}

		add(new ListView<String>("categoriesList", categoryNames) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<String> categoryItem) {
				final String categoryName = categoryItem.getModelObject();

				WebMarkupContainer categoryFilter = new WebMarkupContainer("categoryFilter");
				if (!categoriesEnabled) {
					categoryFilter.add(new AttributeAppender("class", " hide"));
					categoryItem.add(new AttributeAppender("class", " gb-no-categories"));
				}
				categoryItem.add(categoryFilter);

				final GradebookPage gradebookPage = (GradebookPage) getPage();

				final Label categoryLabel = new Label("category", categoryName);
				categoryLabel.add(new AttributeModifier("data-category-color", settings.getCategoryColor(categoryName)));
				categoryFilter.add(categoryLabel);

				categoryFilter.add(new WebMarkupContainer("categorySignal").
					add(new AttributeModifier("style",
						String.format("background-color: %s; border-color: %s",
							settings.getCategoryColor(categoryName),
							settings.getCategoryColor(categoryName)))));

				final CheckBox categoryCheckbox = new CheckBox("categoryCheckbox");
				categoryCheckbox.add(new AttributeModifier("value", categoryName));
				categoryCheckbox.add(new AttributeModifier("checked", "checked"));
				categoryFilter.add(categoryCheckbox);

				categoryItem.add(new ListView<Assignment>("assignmentsForCategory", categoryNamesToAssignments.get(categoryName)) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(final ListItem<Assignment> assignmentItem) {
						final Assignment assignment = assignmentItem.getModelObject();

						final GradebookUiSettings settings = gradebookPage.getUiSettings();

						assignmentItem.add(new Label("assignmentTitle", FormatHelper.abbreviateMiddle(assignment.getName())));

						final WebMarkupContainer assignmentSignal = new WebMarkupContainer("assignmentSignal");
						if (settings.isCategoriesEnabled()) {
							assignmentSignal.add(new AttributeModifier("style",
								String.format("background-color: %s; border-color: %s",
									settings.getCategoryColor(getCategoryName(assignment, categoriesEnabled)),
									settings.getCategoryColor(getCategoryName(assignment, categoriesEnabled)))));
						}
						assignmentItem.add(assignmentSignal);

						final CheckBox assignmentCheckbox = new AjaxCheckBox("assignmentCheckbox",
								Model.of(Boolean.valueOf(settings.isAssignmentVisible(assignment.getId())))) {
							@Override
							protected void onUpdate(final AjaxRequestTarget target) {
								GradebookUiSettings settings = gradebookPage.getUiSettings();

								final Boolean value = settings.isAssignmentVisible(assignment.getId());
								settings.setAssignmentVisibility(assignment.getId(), !value);

								gradebookPage.setUiSettings(settings);
							}
						};
						assignmentCheckbox.add(new AttributeModifier("value", assignment.getId().toString()));
						assignmentCheckbox.add(new AttributeModifier("data-colidx", assignments.indexOf(assignment)));
						assignmentItem.add(assignmentCheckbox);
					}
				});

				final WebMarkupContainer categoryScoreFilter = new WebMarkupContainer("categoryScore");
				categoryScoreFilter.setVisible(categoryName != getString(GradebookPage.UNCATEGORISED));
				categoryScoreFilter.add(new Label("categoryScoreLabel",
						new StringResourceModel("label.toolbar.categoryscorelabel", null, new Object[] { categoryName })));

				categoryScoreFilter.add(new WebMarkupContainer("categoryScoreSignal").
					add(new AttributeModifier("style",
						String.format("background-color: %s; border-color: %s",
							settings.getCategoryColor(categoryName),
							settings.getCategoryColor(categoryName)))));

				final CheckBox categoryScoreCheckbox = new AjaxCheckBox("categoryScoreCheckbox",
						new Model<Boolean>(settings.isCategoryScoreVisible(categoryName))) {// Model.of(Boolean.valueOf(settings.isCategoryScoreVisible(category))))
					// {
					@Override
					protected void onUpdate(final AjaxRequestTarget target) {
						GradebookUiSettings settings = gradebookPage.getUiSettings();

						final Boolean value = settings.isCategoryScoreVisible(categoryName);
						settings.setCategoryScoreVisibility(categoryName, !value);

						gradebookPage.setUiSettings(settings);
					}
				};
				categoryScoreCheckbox.add(new AttributeModifier("value", categoryName));
				categoryScoreFilter.add(categoryScoreCheckbox);

				categoryItem.add(categoryScoreFilter);
			}
		});
	}

	/**
	 * Helper to get the category name. Looks at settings as well.
	 *
	 * @param assignment
	 * @return
	 */
	private String getCategoryName(final Assignment assignment, final boolean categoriesEnabled) {

		if (!categoriesEnabled) {
			return getString(GradebookPage.UNCATEGORISED);
		}

		return StringUtils.isBlank(assignment.getCategoryName()) ? getString(GradebookPage.UNCATEGORISED) : assignment.getCategoryName();
	}
}
