package org.sakaiproject.gradebookng.tool.component;

import java.io.IOException;
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

import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbGradeInfo;
import org.sakaiproject.component.cover.ServerConfigurationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.sakaiproject.gradebookng.tool.model.GbGradebookData;
import org.sakaiproject.gradebookng.tool.actions.Action;
import java.util.HashMap;
import org.sakaiproject.gradebookng.tool.actions.ActionResponse;


public class GbGradeTable extends Panel implements IHeaderContributor {

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;

	private Component component;

	/*
	    - Students: id, first name, last name, netid
	    - Course grades column: is released?, course grade
	    - course grade value for each student (letter, percentage, points)
	    - assignment header: id, points, due date, category {id, name, color}, included in course grade?, external?
	      - categories: enabled?  weighted categories?  normal categories?  handle uncategorized
	    - scores: number, has comments?, extra credit? (> total points), read only?
	 */

	private Map<String, Action> listeners = new HashMap<String, Action>();

	public void addEventListener(String event, Action listener) {
		listeners.put(event, listener);
	}

	public ActionResponse handleEvent(String event, JsonNode params) {
		if (!listeners.containsKey(event)) {
			throw new RuntimeException("Missing AJAX handler");
		}
		
		return listeners.get(event).handleEvent(params);
	}

	public GbGradeTable(String id) {
		super(id);
		
		component = new WebMarkupContainer("gradeTable").setOutputMarkupId(true);

		component.add(new AjaxEventBehavior("gbgradetable.action") {
			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getDynamicExtraParameters().add("return [{\"name\": \"ajaxParams\", \"value\": JSON.stringify(attrs.event.extraData)}]");
			}

			@Override
			protected void onEvent(AjaxRequestTarget target) {
				try {
					ObjectMapper mapper = new ObjectMapper();
					JsonNode params = mapper.readTree(getRequest().getRequestParameters().getParameterValue("ajaxParams").toString());

					ActionResponse response = handleEvent(params.get("action").asText(), params);

					target.appendJavaScript(String.format("GbGradeTable.ajaxComplete(%d, '%s', %s);",
									      params.get("_requestId").intValue(), response.getStatus(), response.toJson()));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

		add(component);
	}

	public void renderHead(IHeaderResponse response) {
		Map<String, Object> model = (Map<String, Object>) getDefaultModelObject();
		List<GbStudentGradeInfo> grades = (List<GbStudentGradeInfo>) model.get("grades");
		List<Assignment> assignments = (List<Assignment>) model.get("assignments");

		final String version = ServerConfigurationService.getString("portal.cdn.version", "");

		response.render(
			JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/gradebook-gbgrade-table.js?version=%s", version)));

		response.render(
			JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/handsontable.full.min.js?version=%s", version)));

		response.render(CssHeaderItem.forUrl(String.format("/gradebookng-tool/styles/handsontable.full.min.css?version=%s", version)));

		GbGradebookData gradebookData = new GbGradebookData(
				grades,
				assignments,
				this.businessService.getGradebookCategories(),
				this.businessService.getGradebookSettings(),
				this);

		response.render(OnDomReadyHeaderItem.forScript(String.format("var tableData = %s", gradebookData.toScript())));

		response.render(OnDomReadyHeaderItem.forScript(String.format("GbGradeTable.renderTable('%s', tableData)",
									     component.getMarkupId())));
	}
}
