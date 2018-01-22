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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseExportMemberCommand<R extends CommandResult> extends BaseExportCommand<R> implements ProvidedProjectModeCommand {
	
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String RECURSIVE = "recursive";
	public static final String ALIGNMENT_NAME = "alignmentName";

	private Expression whereClause;
	private String alignmentName;
	private Boolean recursive;


	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
		recursive = PluginUtils.configureBooleanProperty(configElem, "recursive", true);
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
	}

	protected Expression getWhereClause() {
		return whereClause;
	}

	protected Boolean getRecursive() {
		return recursive;
	}

	protected String getAlignmentName() {
		return alignmentName;
	}
	
	
	
}