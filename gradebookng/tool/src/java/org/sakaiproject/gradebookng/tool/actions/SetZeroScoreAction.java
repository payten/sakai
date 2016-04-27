package org.sakaiproject.gradebookng.tool.actions;


import com.fasterxml.jackson.databind.JsonNode;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.tool.model.GbModalWindow;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.gradebookng.tool.panels.EditGradeCommentPanel;
import org.sakaiproject.gradebookng.tool.panels.ZeroUngradedItemsPanel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SetZeroScoreAction implements Action, Serializable {

    private static final long serialVersionUID = 1L;

    public SetZeroScoreAction() {
    }

    @Override
    public ActionResponse handleEvent(JsonNode params, AjaxRequestTarget target) {
        final GradebookPage gradebookPage = (GradebookPage) target.getPage();

        final GbModalWindow window = gradebookPage.getUpdateUngradedItemsWindow();

        window.setTitle(gradebookPage.getString("heading.zeroungradeditems"));
        window.setReturnFocusToCourseGrade();
        window.setContent(new ZeroUngradedItemsPanel(window.getContentId(), window));
        window.showUnloadConfirmation(false);
        window.show(target);

        return new EmptyOkResponse();
    }
}
