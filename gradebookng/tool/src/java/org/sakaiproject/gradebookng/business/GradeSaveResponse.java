package org.sakaiproject.gradebookng.business;

import lombok.Getter;
import org.sakaiproject.service.gradebook.shared.GradeDefinition;

public class GradeSaveResponse {

	private enum Response {
		OK,
		ERROR,
		OVER_LIMIT,
		NO_CHANGE,
		CONCURRENT_EDIT;
	}

	private Response state;

	@Getter
	private GradeDefinition grade;

	public GradeSaveResponse(Response state, GradeDefinition grade) {
		this.state = state;
		this.grade = grade;
	}

	public GradeSaveResponse(Response state) {
		this.state = state;
		this.grade = null;
	}

	public boolean isOk() {
		return Response.OK.equals(this.state);
	}

	public boolean isError() {
		return Response.ERROR.equals(this.state);
	}

	public boolean isOverLimit() {
		return Response.OVER_LIMIT.equals(this.state);
	}

	public boolean isNoChange() {
		return Response.NO_CHANGE.equals(this.state);
	}
	
	public boolean isConcurrentEdit() {
		return Response.CONCURRENT_EDIT.equals(this.state);
	}

	public static GradeSaveResponse ok(GradeDefinition grade) {
		return new GradeSaveResponse(Response.OK, grade);
	}

	public static GradeSaveResponse error() {
		return new GradeSaveResponse(Response.ERROR);
	}

	public static GradeSaveResponse overLimit() {
		return new GradeSaveResponse(Response.OVER_LIMIT);
	}

	public static GradeSaveResponse overLimit(GradeDefinition grade) {
		return new GradeSaveResponse(Response.OVER_LIMIT, grade);
	}

	public static GradeSaveResponse noChange() {
		return new GradeSaveResponse(Response.NO_CHANGE);
	}

	public static GradeSaveResponse concurrentEdit() {
		return new GradeSaveResponse(Response.CONCURRENT_EDIT);
	}
}
