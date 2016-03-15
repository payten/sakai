package org.sakaiproject.gradebookng.tool.pages;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.business.util.Temp;
import org.sakaiproject.gradebookng.tool.actions.*;
import org.sakaiproject.gradebookng.tool.component.GbGradeTable;
import org.sakaiproject.gradebookng.tool.model.GbGradeTableData;
import org.sakaiproject.gradebookng.tool.model.GbModalWindow;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.panels.AddOrEditGradeItemPanel;
import org.sakaiproject.gradebookng.tool.panels.SortGradeItemsPanel;
import org.sakaiproject.gradebookng.tool.panels.ToggleGradeItemsToolbarPanel;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;
import org.sakaiproject.service.gradebook.shared.SortType;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Grades page. Instructors and TAs see this one. Students see the {@link StudentPage}.
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
public class GradebookPage extends BasePage {
	private static final long serialVersionUID = 1L;

	public static final String CREATED_ASSIGNMENT_ID_PARAM = "createdAssignmentId";

	// flag to indicate a category is uncategorised
	// doubles as a translation key
	public static final String UNCATEGORISED = "gradebookpage.uncategorised";

	GbModalWindow addOrEditGradeItemWindow;
	GbModalWindow studentGradeSummaryWindow;
	GbModalWindow updateUngradedItemsWindow;
	GbModalWindow gradeLogWindow;
	GbModalWindow gradeCommentWindow;
	GbModalWindow deleteItemWindow;
	GbModalWindow gradeStatisticsWindow;
	GbModalWindow updateCourseGradeDisplayWindow;
	GbModalWindow sortGradeItemsWindow;

	Form<Void> form;

	private GbGradeTable gradeTable;

	@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
	public GradebookPage() {
		disableLink(this.gradebookPageLink);

		// students cannot access this page
		if (this.role == GbRole.STUDENT) {
			throw new RestartResponseException(StudentPage.class);
		}

		final StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		Temp.time("GradebookPage init", stopwatch.getTime());

		this.form = new Form<Void>("form");
		add(this.form);

		/**
		 * Note that SEMI_TRANSPARENT has a 100% black background and TRANSPARENT is overridden to 10% opacity
		 */
		this.addOrEditGradeItemWindow = new GbModalWindow("addOrEditGradeItemWindow");
		this.addOrEditGradeItemWindow.showUnloadConfirmation(false);
		this.form.add(this.addOrEditGradeItemWindow);

		this.studentGradeSummaryWindow = new GbModalWindow("studentGradeSummaryWindow");
		this.studentGradeSummaryWindow.setWidthUnit("%");
		this.studentGradeSummaryWindow.setInitialWidth(70);
		this.form.add(this.studentGradeSummaryWindow);

		this.updateUngradedItemsWindow = new GbModalWindow("updateUngradedItemsWindow");
		this.form.add(this.updateUngradedItemsWindow);

		this.gradeLogWindow = new GbModalWindow("gradeLogWindow");
		this.form.add(this.gradeLogWindow);

		this.gradeCommentWindow = new GbModalWindow("gradeCommentWindow");
		this.form.add(this.gradeCommentWindow);

		this.sortGradeItemsWindow = new GbModalWindow("sortGradeItemsWindow");
		this.sortGradeItemsWindow.showUnloadConfirmation(false);
		this.form.add(this.sortGradeItemsWindow);

		this.deleteItemWindow = new GbModalWindow("deleteItemWindow");
		this.form.add(this.deleteItemWindow);

		this.gradeStatisticsWindow = new GbModalWindow("gradeStatisticsWindow");
		this.gradeStatisticsWindow.setPositionAtTop(true);
		this.form.add(this.gradeStatisticsWindow);

		this.updateCourseGradeDisplayWindow = new GbModalWindow("updateCourseGradeDisplayWindow");
		this.form.add(this.updateCourseGradeDisplayWindow);

		final AjaxButton addGradeItem = new AjaxButton("addGradeItem") {
			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form form) {
				final GbModalWindow window = getAddOrEditGradeItemWindow();
				window.setTitle(getString("heading.addgradeitem"));
				window.setComponentToReturnFocusTo(this);
				window.setContent(new AddOrEditGradeItemPanel(window.getContentId(), window, null));
				window.show(target);
			}

			@Override
			public boolean isVisible() {
				if (GradebookPage.this.role != GbRole.INSTRUCTOR) {
					return false;
				}
				return true;
			}

		};
		addGradeItem.setDefaultFormProcessing(false);
		addGradeItem.setOutputMarkupId(true);
		this.form.add(addGradeItem);

		// first get any settings data from the session
		final GradebookUiSettings settings = getUiSettings();

		SortType sortBy = SortType.SORT_BY_SORTING;
		if (settings.isCategoriesEnabled()) {
			// Pre-sort assignments by the categorized sort order
			sortBy = SortType.SORT_BY_CATEGORY;
			this.form.add(new AttributeAppender("class", "gb-grouped-by-category"));
		}

		// get Gradebook to save additional calls later
		final Gradebook gradebook = this.businessService.getGradebook();

		// categories enabled?
		final boolean categoriesEnabled = this.businessService.categoriesAreEnabled();

		Temp.time("all Columns added", stopwatch.getTime());

		gradeTable = new GbGradeTable("gradeTable",
					      new LoadableDetachableModel() {
						      @Override
						      public GbGradeTableData load() {
							      return new GbGradeTableData(businessService, settings);
						      }
					      });
		gradeTable.addEventListener("setScore", new GradeUpdateAction(this.businessService));
		gradeTable.addEventListener("viewLog", new ViewGradeLogAction(this.businessService));
		gradeTable.addEventListener("editAssignment", new EditAssignmentAction(this.businessService));
		gradeTable.addEventListener("viewStatistics", new ViewAssignmentStatisticsAction(this.businessService));
		gradeTable.addEventListener("overrideCourseGrade", new OverrideCourseGradeAction(this.businessService));
		gradeTable.addEventListener("editComment", new EditCommentAction(this.businessService));
		gradeTable.addEventListener("viewGradeSummary", new ViewGradeSummaryAction(this.businessService));
		gradeTable.addEventListener("setZeroScore", new SetZeroScoreAction());
		gradeTable.addEventListener("viewCourseGradeLog", new ViewCourseGradeLogAction());
		gradeTable.addEventListener("deleteAssignment", new DeleteAssignmentAction());
		gradeTable.addEventListener("setUngraded", new SetScoreForUngradedAction());

		this.form.add(gradeTable);

		final WebMarkupContainer toggleGradeItemsToolbarItem = new WebMarkupContainer("toggleGradeItemsToolbarItem");
		this.form.add(toggleGradeItemsToolbarItem);

		final AjaxLink toggleCategoriesToolbarItem = new AjaxLink("toggleCategoriesToolbarItem") {
			@Override
			protected void onInitialize() {
				super.onInitialize();
				if (settings.isCategoriesEnabled()) {
					add(new AttributeAppender("class", " on"));
				}
				add(new AttributeModifier("aria-pressed", settings.isCategoriesEnabled()));
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				settings.setCategoriesEnabled(!settings.isCategoriesEnabled());
				setUiSettings(settings);

				// refresh
				setResponsePage(new GradebookPage());
			}

			@Override
			public boolean isVisible() {
				return categoriesEnabled;
			}
		};
		this.form.add(toggleCategoriesToolbarItem);

		final AjaxLink sortGradeItemsToolbarItem = new AjaxLink("sortGradeItemsToolbarItem") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				GbModalWindow window = GradebookPage.this.getSortGradeItemsWindow();

				Map<String, Object> model = new HashMap<>();
				model.put("categoriesEnabled", categoriesEnabled);
				model.put("settings", settings);

				window.setTitle(getString("sortgradeitems.heading"));
				window.setContent(new SortGradeItemsPanel(window.getContentId(), Model.ofMap(model), window));
				window.setComponentToReturnFocusTo(this);
				window.show(target);
			}

			@Override
			public boolean isVisible() {
				return GradebookPage.this.role == GbRole.INSTRUCTOR;
			}
		};
		this.form.add(sortGradeItemsToolbarItem);

		// section and group dropdown
		final List<GbGroup> groups = this.businessService.getSiteSectionsAndGroups();

		// if only one group, just show the title
		// otherwise add the 'all groups' option
		if (groups.size() == 1) {
			this.form.add(new Label("groupFilterOnlyOne", Model.of(groups.get(0).getTitle())));
		} else {
			this.form.add(new EmptyPanel("groupFilterOnlyOne").setVisible(false));

			// add the default ALL group to the list
			String allGroupsTitle = getString("groups.all");
			if (this.role == GbRole.TA) {

				// does the TA have any permissions set?
				// we can assume that if they have any then there is probably some sort of group restriction so we can change the label
				if (!this.businessService.getPermissionsForUser(this.currentUserUuid).isEmpty()) {
					allGroupsTitle = getString("groups.available");
				}
			}
			groups.add(0, new GbGroup(null, allGroupsTitle, null, GbGroup.Type.ALL));

		}

		final DropDownChoice<GbGroup> groupFilter = new DropDownChoice<GbGroup>("groupFilter", new Model<GbGroup>(),
				groups, new ChoiceRenderer<GbGroup>() {
					private static final long serialVersionUID = 1L;

					@Override
					public Object getDisplayValue(final GbGroup g) {
						return g.getTitle();
					}

					@Override
					public String getIdValue(final GbGroup g, final int index) {
						return g.getId();
					}

				});

		groupFilter.add(new AjaxFormComponentUpdatingBehavior("onchange") {

			@Override
			protected void onUpdate(final AjaxRequestTarget target) {

				final GbGroup selected = (GbGroup) groupFilter.getDefaultModelObject();

				// store selected group (null ok)
				final GradebookUiSettings settings = getUiSettings();
				settings.setGroupFilter(selected);
				setUiSettings(settings);

				// refresh
				setResponsePage(new GradebookPage());
			}

		});

		// set selected group, or first item in list
		groupFilter.setModelObject((settings.getGroupFilter() != null) ? settings.getGroupFilter() : groups.get(0));
		groupFilter.setNullValid(false);

		// if only one item, hide the dropdown
		if (groups.size() == 1) {
			groupFilter.setVisible(false);
		}

		this.form.add(groupFilter);

		final Map<String, Object> togglePanelModel = new HashMap<>();
		togglePanelModel.put("assignments", this.businessService.getGradebookAssignments(sortBy));
		togglePanelModel.put("settings", settings);
		togglePanelModel.put("categoriesEnabled", categoriesEnabled);

		final ToggleGradeItemsToolbarPanel gradeItemsTogglePanel =
			new ToggleGradeItemsToolbarPanel("gradeItemsTogglePanel", Model.ofMap(togglePanelModel));
		add(gradeItemsTogglePanel);

		Temp.time("Gradebook page done", stopwatch.getTime());
	}

	/**
	 * Getters for panels to get at modal windows
	 *
	 * @return
	 */
	public GbModalWindow getAddOrEditGradeItemWindow() {
		return this.addOrEditGradeItemWindow;
	}

	public GbModalWindow getStudentGradeSummaryWindow() {
		return this.studentGradeSummaryWindow;
	}

	public GbModalWindow getUpdateUngradedItemsWindow() {
		return this.updateUngradedItemsWindow;
	}

	public GbModalWindow getGradeLogWindow() {
		return this.gradeLogWindow;
	}

	public GbModalWindow getGradeCommentWindow() {
		return this.gradeCommentWindow;
	}

	public GbModalWindow getDeleteItemWindow() {
		return this.deleteItemWindow;
	}

	public GbModalWindow getGradeStatisticsWindow() {
		return this.gradeStatisticsWindow;
	}

	public GbModalWindow getUpdateCourseGradeDisplayWindow() {
		return this.updateCourseGradeDisplayWindow;
	}

	public GbModalWindow getSortGradeItemsWindow() {
		return this.sortGradeItemsWindow;
	}

	/**
	 * Getter for the GradebookUiSettings. Used to store a few UI related settings for the current session only.
	 *
	 * TODO move this to a helper
	 */
	public GradebookUiSettings getUiSettings() {

		GradebookUiSettings settings = (GradebookUiSettings) Session.get().getAttribute("GBNG_UI_SETTINGS");

		if (settings == null) {
			settings = new GradebookUiSettings();
			settings.setCategoriesEnabled(this.businessService.categoriesAreEnabled());
			settings.initializeCategoryColors(this.businessService.getGradebookCategories());
			settings.setCategoryColor(getString(GradebookPage.UNCATEGORISED), settings.generateRandomRGBColorString());
			setUiSettings(settings);
		}

		return settings;
	}

	public void setUiSettings(final GradebookUiSettings settings) {
		Session.get().setAttribute("GBNG_UI_SETTINGS", settings);
	}

	@Override
	public void renderHead(final IHeaderResponse response) {
		super.renderHead(response);

		final String version = ServerConfigurationService.getString("portal.cdn.version", "");

		// Drag and Drop/Date Picker (requires jQueryUI)
		response.render(JavaScriptHeaderItem
				.forUrl(String.format("/library/webjars/jquery-ui/1.11.3/jquery-ui.min.js?version=%s", version)));

		// Include Sakai Date Picker
		response.render(JavaScriptHeaderItem
				.forUrl(String.format("/library/js/lang-datepicker/lang-datepicker.js?version=%s", version)));

		// GradebookNG Grade specific styles and behaviour
		response.render(CssHeaderItem
			.forUrl(String.format("/gradebookng-tool/styles/gradebook-grades.css?version=%s", version)));
		response.render(CssHeaderItem
			.forUrl(String.format("/gradebookng-tool/styles/gradebook-print.css?version=%s", version), "print"));
		response.render(CssHeaderItem
			.forUrl(String.format("/gradebookng-tool/styles/gradebook-sorter.css?version=%s", version)));
		response.render(JavaScriptHeaderItem.
			forUrl(String.format("/gradebookng-tool/scripts/gradebook-grade-summary.js?version=%s", version)));
		response.render(JavaScriptHeaderItem.
			forUrl(String.format("/gradebookng-tool/scripts/gradebook-update-ungraded.js?version=%s", version)));
		response.render(JavaScriptHeaderItem.
			forUrl(String.format("/gradebookng-tool/scripts/gradebook-sorter.js?version=%s", version)));
	}

	/**
	 * Build a table row summary for the table
	 */
	private Label constructTableSummaryLabel(final String componentId, final DataTable table) {
		return constructTableLabel(componentId, table, false);
	}

	/**
	 * Build a table pagination summary for the table
	 */
	private Label constructTablePaginationLabel(final String componentId, final DataTable table) {
		return constructTableLabel(componentId, table, true);
	}

	/**
	 * Build a table summary for the table along the lines of if verbose: "Showing 1{from} to 100{to} of 153{of} students" else:
	 * "Showing 100{to} students"
	 */
	private Label constructTableLabel(final String componentId, final DataTable table, final boolean verbose) {
		final long of = table.getItemCount();
		final long from = (of == 0 ? 0 : table.getCurrentPage() * table.getItemsPerPage() + 1);
		final long to = (of == 0 ? 0 : Math.min(of, from + table.getItemsPerPage() - 1));

		StringResourceModel labelText;

		if (verbose) {
			labelText = new StringResourceModel("label.toolbar.studentsummarypaginated", null, from, to, of);
		} else {
			labelText = new StringResourceModel("label.toolbar.studentsummary", null, to);
		}

		final Label label = new Label(componentId, labelText);
		label.setEscapeModelStrings(false); // to allow embedded HTML

		return label;
	}

	/**
	 * Comparator class for sorting Assignments in their categorised ordering
	 */
	class CategorizedAssignmentComparator implements Comparator<Assignment> {
		@Override
		public int compare(final Assignment a1, final Assignment a2) {
			// if in the same category, sort by their categorized sort order
			if (a1.getCategoryId() == a2.getCategoryId()) {
				// handles null orders by putting them at the end of the list
				if (a1.getCategorizedSortOrder() == null) {
					return 1;
				} else if (a2.getCategorizedSortOrder() == null) {
					return -1;
				}
				return Integer.compare(a1.getCategorizedSortOrder(), a2.getCategorizedSortOrder());

				// otherwise, sort by their category order
			} else {
				if (a1.getCategoryOrder() == null && a2.getCategoryOrder() == null) {
					// both orders are null.. so order by A-Z
					if (a1.getCategoryName() == null && a2.getCategoryName() == null) {
						// both names are null so order by id
						return a1.getCategoryId().compareTo(a2.getCategoryId());
					} else if (a1.getCategoryName() == null) {
						return 1;
					} else if (a2.getCategoryName() == null) {
						return -1;
					} else {
						return a1.getCategoryName().compareTo(a2.getCategoryName());
					}
				} else if (a1.getCategoryOrder() == null) {
					return 1;
				} else if (a2.getCategoryOrder() == null) {
					return -1;
				} else {
					return a1.getCategoryOrder().compareTo(a2.getCategoryOrder());
				}
			}
		}
	}
}
