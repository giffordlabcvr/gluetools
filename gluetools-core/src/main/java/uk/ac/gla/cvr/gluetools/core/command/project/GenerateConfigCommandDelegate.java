/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
