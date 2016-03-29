package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class GenerateConfigCommandDelegate {

	public static final String NO_COMMIT = "noCommit";
	public static final String COMMIT_AT_END = "commitAtEnd";
	public static final String FILE_NAME = "fileName";
	public static final String PREVIEW = "preview";

	private boolean noCommit;
	private boolean commitAtEnd;
	private String fileName;
	private boolean preview;

	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
		commitAtEnd = PluginUtils.configureBooleanProperty(configElem, COMMIT_AT_END, true);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		if( (fileName == null && !preview) || (fileName != null && preview) ) {
			usageError1();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fileName> or --preview must be specified, but not both");
	}

	public boolean getNoCommit() {
		return noCommit;
	}

	public boolean getCommitAtEnd() {
		return commitAtEnd;
	}

	public String getFileName() {
		return fileName;
	}

	public boolean getPreview() {
		return preview;
	}


	
}
