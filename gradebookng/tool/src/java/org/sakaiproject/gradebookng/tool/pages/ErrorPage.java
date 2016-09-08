package org.sakaiproject.gradebookng.tool.pages;

import lombok.extern.slf4j.Slf4j;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.sakaiproject.portal.util.ErrorReporter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Page displayed when an internal error occurred.
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
@Slf4j
public class ErrorPage extends BasePage {

	private static final long serialVersionUID = 1L;

	private Exception exception;

	public ErrorPage(final Exception e) {
		this.exception = e;
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		final HttpServletRequest request = ((HttpServletRequest) getRequest().getContainerRequest());
		final HttpServletResponse response = ((HttpServletResponse) getResponse().getContainerResponse());

		new ErrorReporter().report(request, response, this.exception);
	}

	@Override
	public void renderHead(final IHeaderResponse response) {
		// Force a redirect back to the Grade page.
		// This should never happen as the ErrorReporter.report
		// above should hijack the response when it counts.
		response.render(new StringHeaderItem("<META http-equiv=\"refresh\"" + 
			" content=\"0;URL=" +
			getRequestCycle().urlFor(GradebookPage.class, null) + 
			"\">"));
	}
}
