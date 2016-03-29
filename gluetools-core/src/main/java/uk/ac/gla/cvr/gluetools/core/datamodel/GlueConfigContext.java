package uk.ac.gla.cvr.gluetools.core.datamodel;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public class GlueConfigContext {
	private CommandContext cmdContext;
	private boolean includeVariations;
	private boolean noCommit;
	private boolean commitAtEnd;
	public GlueConfigContext(CommandContext cmdContext,
			boolean includeVariations, boolean noCommit, boolean commitAtEnd) {
		super();
		this.cmdContext = cmdContext;
		this.includeVariations = includeVariations;
		this.noCommit = noCommit;
		this.commitAtEnd = commitAtEnd;
	}
	
	public CommandContext getCommandContext() {
		return cmdContext;
	}
	public boolean getIncludeVariations() {
		return includeVariations;
	}
	public boolean getNoCommit() {
		return noCommit;
	}
	public boolean getCommitAtEnd() {
		return commitAtEnd;
	}

	public Project getProject() {
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		return insideProjectMode.getProject();
	}
	
	
}
