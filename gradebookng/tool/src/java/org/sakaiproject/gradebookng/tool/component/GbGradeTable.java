package org.sakaiproject.gradebookng.tool.component;

import java.io.UnsupportedEncodingException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.WebApplication;

import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbGradeInfo;
import org.sakaiproject.component.cover.ServerConfigurationService;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;


public class GbGradeTable extends Panel implements IHeaderContributor {

	private Component component;
	private List<Long> assignments;
	private List<String> students;
	private List<String> grades;

	public GbGradeTable(String id, List<GbStudentGradeInfo> grades, List<Assignment> assignments) {
		super(id);
		
		this.assignments = extractAssignments(assignments);
		this.students = extractStudents(grades);
		this.grades = extractGrades(grades);

		component = new WebMarkupContainer("gradeTable").setOutputMarkupId(true);

		component.add(new AjaxEventBehavior("gbgradetable.action") {

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getDynamicExtraParameters().add("return [{\"name\": \"ajaxParams\", \"value\": JSON.stringify(attrs.event.extraData)}]");
			}

			@Override
			protected void onEvent(AjaxRequestTarget target) {
				System.err.println("GOT PARAMS: " + getRequest().getRequestParameters().getParameterValue("ajaxParams"));
			}
		});

		add(component);
	}

	public void renderHead(IHeaderResponse response) {
		final String version = ServerConfigurationService.getString("portal.cdn.version", "");

		response.render(
			JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/gradebook-gbgrade-table.js?version=%s", version)));

		response.render(
			JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/handsontable.full.min.js?version=%s", version)));

		response.render(CssHeaderItem.forUrl(String.format("/gradebookng-tool/styles/handsontable.full.min.css?version=%s", version)));

		response.render(OnDomReadyHeaderItem.forScript(String.format("var tableData = GbGradeTable.unpack('%s', %d, %d)",
									     serializedGrades(),
									     this.students.size(),
									     this.assignments.size())));

		response.render(OnDomReadyHeaderItem.forScript(String.format("GbGradeTable.renderTable('%s', %s, %s, tableData)",
									     component.getMarkupId(),
									     jsonAssignments(),
									     jsonStudents())));
	}

	private List<Long> extractAssignments(List<Assignment> assignments) {
		List<Long> result = new ArrayList<Long>();

		for (Assignment assignment : assignments) {
			result.add(assignment.getId());
		}

		return result;
	}

	private List<String> extractStudents(List<GbStudentGradeInfo> grades) {
		List<String> result = new ArrayList<String>();

		for (GbStudentGradeInfo studentGrades : grades) {
			result.add(studentGrades.getStudentEid());
		}

		return result;
	}

	private List<String> extractGrades(List<GbStudentGradeInfo> grades) {
		List<String> result = new ArrayList<String>();

		for (GbStudentGradeInfo studentGradeInfo : grades) {
			Map<Long, GbGradeInfo> studentGrades = studentGradeInfo.getGrades();

			for (Long assignmentId : this.assignments) {
				GbGradeInfo gradeInfo = studentGrades.get(assignmentId);

				if (gradeInfo == null) {
					result.add("0");
				} else {
					String grade = gradeInfo.getGrade();
					result.add((grade == null) ? "0" : grade);
				}
			}
		}

		return result;
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

	private String serializedGrades() {
		StringBuilder sb = new StringBuilder();

		for (String gradeString : this.grades) {
			double grade = Double.valueOf(gradeString);

			boolean hasFraction = ((int)grade != grade);

			if (grade < 128 && !hasFraction) {
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

	// FIXME: stupid
	private String jsonAssignments() {
		StringBuilder sb = new StringBuilder();

		for (Long assignmentId : this.assignments) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(assignmentId.toString());
		}

		return "[" + sb.toString() + "]";
	}

	// FIXME: double stupid
	private String jsonStudents() {
		StringBuilder sb = new StringBuilder();

		for (String student : this.students) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("\"" + student.toString() + "\"");
		}



		return "[" + sb.toString() + "]";
	}

	// Want to send:
	//   - the list of all known assignments
	//   - the list of all known students
	//   - a giant array where each sequence of assignments.length is the score for a given student.
}

