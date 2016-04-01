package org.sakaiproject.gradebookng.tool.pages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.time.StopWatch;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.business.util.Temp;
import org.sakaiproject.gradebookng.tool.component.GbGradeTable;
import org.sakaiproject.gradebookng.tool.model.GbGradeTableData;
import org.sakaiproject.gradebookng.tool.model.GbModalWindow;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.panels.AddOrEditGradeItemPanel;
import org.sakaiproject.gradebookng.tool.panels.ToggleGradeItemsToolbarPanel;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;
import org.sakaiproject.service.gradebook.shared.SortType;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.gradebookng.tool.actions.GradeUpdateAction;
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

		// this could potentially be a sortable data provider
		// final ListDataProvider<GbStudentGradeInfo> studentGradeMatrix = new ListDataProvider<GbStudentGradeInfo>(grades);
		// final List<IColumn> cols = new ArrayList<IColumn>();

		// // add an empty column that we can use as a handle for selecting the row
		// final AbstractColumn handleColumn = new AbstractColumn(new Model("")) {
		// 
		// 	@Override
		// 	public void populateItem(final Item cellItem, final String componentId, final IModel rowModel) {
		// 		cellItem.add(new EmptyPanel(componentId));
		// 	}
		// 
		// 	@Override
		// 	public String getCssClass() {
		// 		return "gb-row-selector";
		// 	}
		// };
		// cols.add(handleColumn);

		// student name column
		// final AbstractColumn studentNameColumn = new AbstractColumn(new Model("studentColumn")) {
		// 
		// 	@Override
		// 	public Component getHeader(final String componentId) {
		// 		return new StudentNameColumnHeaderPanel(componentId, Model.of(settings.getNameSortOrder())); // pass in the sort
		// 	}
		// 
		// 	@Override
		// 	public void populateItem(final Item cellItem, final String componentId, final IModel rowModel) {
		// 		final GbStudentGradeInfo studentGradeInfo = (GbStudentGradeInfo) rowModel.getObject();
		// 
		// 		final Map<String, Object> modelData = new HashMap<>();
		// 		modelData.put("userId", studentGradeInfo.getStudentUuid());
		// 		modelData.put("eid", studentGradeInfo.getStudentEid());
		// 		modelData.put("firstName", studentGradeInfo.getStudentFirstName());
		// 		modelData.put("lastName", studentGradeInfo.getStudentLastName());
		// 		modelData.put("displayName", studentGradeInfo.getStudentDisplayName());
		// 		modelData.put("nameSortOrder", settings.getNameSortOrder()); // pass in the sort
		// 
		// 		cellItem.add(new StudentNameCellPanel(componentId, Model.ofMap(modelData)));
		// 		cellItem.add(new AttributeModifier("data-studentUuid", studentGradeInfo.getStudentUuid()));
		// 		cellItem.add(new AttributeModifier("abbr", studentGradeInfo.getStudentDisplayName()));
		// 		cellItem.add(new AttributeModifier("aria-label", studentGradeInfo.getStudentDisplayName()));
		// 
		// 		// TODO may need a subclass of Item that does the onComponentTag override and then tag.setName("th");
		// 	}
		// 
		// 	@Override
		// 	public String getCssClass() {
		// 		return "gb-student-cell";
		// 	}
		// 
		// };
		// cols.add(studentNameColumn);

		// course grade column
		// final AbstractColumn courseGradeColumn = new AbstractColumn(new Model("")) {
		// 	@Override
		// 	public Component getHeader(final String componentId) {
		// 		return new CourseGradeColumnHeaderPanel(componentId, Model.of(settings.getShowPoints()));
		// 	}
		// 
		// 	@Override
		// 	public String getCssClass() {
		// 		return "gb-course-grade";
		// 	}
		// 
		// 	@Override
		// 	public void populateItem(final Item cellItem, final String componentId, final IModel rowModel) {
		// 		final GbStudentGradeInfo studentGradeInfo = (GbStudentGradeInfo) rowModel.getObject();
		// 
		// 		cellItem.add(new AttributeModifier("tabindex", 0));
		// 
		// 		// setup model
		// 		// note we have additional fields here fornthe course grade model
		// 		final Map<String, Object> modelData = new HashMap<>();
		// 		modelData.put("courseGrade", studentGradeInfo.getCourseGrade());
		// 		modelData.put("studentUuid", studentGradeInfo.getStudentUuid());
		// 		modelData.put("currentUserUuid", GradebookPage.this.currentUserUuid);
		// 		modelData.put("currentUserRole", GradebookPage.this.role);
		// 		modelData.put("gradebook", gradebook);
		// 		modelData.put("showPoints", settings.getShowPoints());
		// 		modelData.put("showOverride", true);
		// 
		// 		cellItem.add(new CourseGradeItemCellPanel(componentId, Model.ofMap(modelData)));
		// 		cellItem.setOutputMarkupId(true);
		// 	}
		// };
		// cols.add(courseGradeColumn);

		// build the rest of the columns based on the assignment list
		// for (final Assignment assignment : assignments) {
		// 
		// 	final AbstractColumn column = new AbstractColumn(new Model(assignment)) {
		// 
		// 		@Override
		// 		public Component getHeader(final String componentId) {
		// 			final AssignmentColumnHeaderPanel panel = new AssignmentColumnHeaderPanel(componentId,
		// 					new Model<Assignment>(assignment));
		// 
		// 			panel.add(new AttributeModifier("data-category", assignment.getCategoryName()));
		// 			panel.add(new AttributeModifier("data-category-id", assignment.getCategoryId()));
		// 
		// 			final StringValue createdAssignmentId = getPageParameters().get(CREATED_ASSIGNMENT_ID_PARAM);
		// 			if (!createdAssignmentId.isNull() && assignment.getId().equals(createdAssignmentId.toLong())) {
		// 				panel.add(new AttributeModifier("class", "gb-just-created"));
		// 				getPageParameters().remove(CREATED_ASSIGNMENT_ID_PARAM);
		// 			}
		// 
		// 			return panel;
		// 		}
		// 
		// 		@Override
		// 		public String getCssClass() {
		// 			return "gb-grade-item-column-cell";
		// 		}
		// 
		// 		@Override
		// 		public void populateItem(final Item cellItem, final String componentId, final IModel rowModel) {
		// 			final GbStudentGradeInfo studentGrades = (GbStudentGradeInfo) rowModel.getObject();
		// 
		// 			final GbGradeInfo gradeInfo = studentGrades.getGrades().get(assignment.getId());
		// 
		// 			final Map<String, Object> modelData = new HashMap<>();
		// 			modelData.put("assignmentId", assignment.getId());
		// 			modelData.put("assignmentName", assignment.getName());
		// 			modelData.put("assignmentPoints", assignment.getPoints());
		// 			modelData.put("studentUuid", studentGrades.getStudentUuid());
		// 			modelData.put("studentName", studentGrades.getStudentDisplayName());
		// 			modelData.put("categoryId", assignment.getCategoryId());
		// 			modelData.put("isExternal", assignment.isExternallyMaintained());
		// 			modelData.put("externalAppName", assignment.getExternalAppName());
		// 			modelData.put("gradeInfo", gradeInfo);
		// 			modelData.put("role", GradebookPage.this.role);
		// 
		// 			cellItem.add(new GradeItemCellPanel(componentId, Model.ofMap(modelData)));
		// 
		// 			cellItem.setOutputMarkupId(true);
		// 		}
		// 
		// 	};
		// 
		// 	cols.add(column);
		// }

		// render the categories
		// Display rules:
		// 1. only show categories if the global setting is enabled
		// 2. only show categories if they have items
		// TODO may be able to pass this list into the matrix to save another lookup in there)

		List<CategoryDefinition> categories = new ArrayList<>();

		// if (categoriesEnabled) {
		// 
		// 	// only work with categories if enabled
		// 	categories = this.businessService.getGradebookCategories();
		// 
		// 	// remove those that have no assignments
		// 	categories.removeIf(cat -> cat.getAssignmentList().isEmpty());
		// 
		// 	Collections.sort(categories, CategoryDefinition.orderComparator);
		// 
		// 	int currentColumnIndex = 3; // take into account first three header columns
		// 
		// 	for (final CategoryDefinition category : categories) {
		// 
		// 		if (category.getAssignmentList().isEmpty()) {
		// 			continue;
		// 		}
		// 
		// 		final AbstractColumn column = new AbstractColumn(new Model(category)) {
		// 
		// 			@Override
		// 			public Component getHeader(final String componentId) {
		// 				final CategoryColumnHeaderPanel panel = new CategoryColumnHeaderPanel(componentId,
		// 						new Model<CategoryDefinition>(category));
		// 
		// 				panel.add(new AttributeModifier("data-category", category.getName()));
		// 
		// 				return panel;
		// 			}
		// 
		// 			@Override
		// 			public void populateItem(final Item cellItem, final String componentId, final IModel rowModel) {
		// 				final GbStudentGradeInfo studentGrades = (GbStudentGradeInfo) rowModel.getObject();
		// 
		// 				final Double score = studentGrades.getCategoryAverages().get(category.getId());
		// 
		// 				final Map<String, Object> modelData = new HashMap<>();
		// 				modelData.put("score", score);
		// 				modelData.put("studentUuid", studentGrades.getStudentUuid());
		// 				modelData.put("categoryId", category.getId());
		// 
		// 				cellItem.add(new CategoryColumnCellPanel(componentId, Model.ofMap(modelData)));
		// 				cellItem.setOutputMarkupId(true);
		// 			}
		// 
		// 			@Override
		// 			public String getCssClass() {
		// 				return "gb-category-item-column-cell";
		// 			}
		// 
		// 		};
		// 
		// 		if (settings.isCategoriesEnabled()) {
		// 			// insert category column after assignments in that category
		// 			currentColumnIndex = currentColumnIndex + category.getAssignmentList().size();
		// 			cols.add(currentColumnIndex, column);
		// 			currentColumnIndex = currentColumnIndex + 1;
		// 		} else {
		// 			// add to the end of the column list
		// 			cols.add(column);
		// 		}
		// 	}
		// }

		Temp.time("all Columns added", stopwatch.getTime());

		gradeTable = new GbGradeTable("gradeTable",
					      new LoadableDetachableModel() {
						      @Override
						      public GbGradeTableData load() {
							      return new GbGradeTableData(businessService, settings);
						      }
					      });
		gradeTable.addEventListener("setScore", new GradeUpdateAction(this.businessService));

		this.form.add(gradeTable);


		// final Label gradeItemSummary = new Label("gradeItemSummary", new StringResourceModel("label.toolbar.gradeitemsummary", null,
		// 		assignments.size() + categories.size(), assignments.size() + categories.size()));
		// gradeItemSummary.setEscapeModelStrings(false);
		// this.form.add(gradeItemSummary);

		final WebMarkupContainer toggleGradeItemsToolbarItem = new WebMarkupContainer("toggleGradeItemsToolbarItem");
		toggleGradeItemsToolbarItem.setVisible(false);
		this.form.add(toggleGradeItemsToolbarItem);

		final Button toggleCategoriesToolbarItem = new Button("toggleCategoriesToolbarItem") {
			@Override
			protected void onInitialize() {
				super.onInitialize();
				if (settings.isCategoriesEnabled()) {
					add(new AttributeModifier("class", "on"));
				}
				add(new AttributeModifier("aria-pressed", settings.isCategoriesEnabled()));
			}

			@Override
			public void onSubmit() {
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

		// section and group dropdown
		final List<GbGroup> groups = this.businessService.getSiteSectionsAndGroups();

		// add the default ALL group to the list
		groups.add(0, new GbGroup(null, getString("groups.all"), null, GbGroup.Type.ALL));

		final DropDownChoice<GbGroup> groupFilter = new DropDownChoice<GbGroup>("groupFilter", new Model<GbGroup>(), groups,
				new ChoiceRenderer<GbGroup>() {
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
		groupFilter.setVisible(false);
		this.form.add(groupFilter);

		final List<Assignment> assignments = this.businessService.getGradebookAssignments(sortBy);
		final ToggleGradeItemsToolbarPanel gradeItemsTogglePanel = new ToggleGradeItemsToolbarPanel("gradeItemsTogglePanel",
													    Model.ofList(assignments));
		gradeItemsTogglePanel.setVisible(false);
		add(gradeItemsTogglePanel);

		// hide/show components

		Temp.time("Gradebook page done", stopwatch.getTime());
	}

	// @Override
	// protected void onBeforeRender() {
	// 	super.onBeforeRender();
	// 
	// 	// get list of assignments. this allows us to build the columns and then fetch the grades for each student for each assignment from
	// 	// the map
	// 	// first get any settings data from the session
	// 	final GradebookUiSettings settings = getUiSettings();
	// 
	// 	final StopWatch stopwatch = new StopWatch();
	// 	stopwatch.start();
	// 
	// 	final List<Assignment> assignments = this.businessService.getGradebookAssignments(sortBy);
	// 	Temp.time("getGradebookAssignments", stopwatch.getTime());
	// 
	// 	// get the grade matrix. It should be sorted if we have that info
	// 	final List<GbStudentGradeInfo> grades = this.businessService.buildGradeMatrix(assignments,
	// 										      settings.getAssignmentSortOrder(), settings.getNameSortOrder(), settings.getCategorySortOrder(),
	// 										      settings.getGroupFilter());
	// 
	// 	Temp.time("buildGradeMatrix", stopwatch.getTime());
	// 
	// 	Map<String, Object> model = (Map<String, Object>) gradeTable.getDefaultModelObject();
	// 	((List<GbStudentGradeInfo>) model.get("grades")).clear();
	// 	((List<GbStudentGradeInfo>) model.get("grades")).addAll(grades);
	// 
	// 	((List<Assignment>) model.get("assignments")).clear();
	// 	((List<Assignment>) model.get("assignments")).addAll(assignments);
	// 
	// 	gradeTable.modelChanged();
	// }

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
		response.render(
				JavaScriptHeaderItem.forUrl(String.format("/library/webjars/jquery-ui/1.11.3/jquery-ui.min.js?version=%s", version)));

		// Include Sakai Date Picker
		response.render(JavaScriptHeaderItem.forUrl(String.format("/library/js/lang-datepicker/lang-datepicker.js?version=%s", version)));

		// GradebookNG Grade specific styles and behaviour
		response.render(CssHeaderItem.forUrl(String.format("/gradebookng-tool/styles/gradebook-grades.css?version=%s", version)));
		response.render(CssHeaderItem.forUrl(String.format("/gradebookng-tool/styles/gradebook-gbgrade-table.css?version=%s", version)));
		response.render(JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/gradebook-grades.js?version=%s", version)));
		response.render(
				JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/gradebook-grade-summary.js?version=%s", version)));
		response.render(
				JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/gradebook-update-ungraded.js?version=%s", version)));
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
			labelText = new StringResourceModel("label.toolbar.studentsummarypaginated",
					null,
					from, to, of);
		} else {
			labelText = new StringResourceModel("label.toolbar.studentsummary",
					null,
					to);
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
