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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.ExpressionFactory;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"set", "parent"},
		docoptUsages={"<parentAlignmentName>"},
		metaTags = {},
		description="Specify the parent for this alignment",
		furtherHelp="The reference sequence of this alignment must be a member of the parent alignment. "+
				"\nLoops arising from alignment parent relationships are not allowed."
	) 
public class AlignmentSetParentCommand extends AlignmentModeCommand<OkResult> {

	public static final String PARENT_ALIGNMENT_NAME = "parentAlignmentName";
	private String parentAlignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		parentAlignmentName = PluginUtils.configureStringProperty(configElem, PARENT_ALIGNMENT_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		Alignment parentAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(parentAlignmentName));
		alignment.setParent(parentAlignment);
		cmdContext.commit();
		return new OkResult();
	}

	

	@CompleterClass
	public static class AlignmentNameCompleter extends AdvancedCmdCompleter {

		public AlignmentNameCompleter() {
			super();
			registerVariableInstantiator("parentAlignmentName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					String thisAlmtName = ((AlignmentMode) cmdContext.peekCommandMode()).getAlignmentName();
					return listNames(cmdContext, prefix, Alignment.class, Alignment.NAME_PROPERTY, 
							ExpressionFactory.noMatchExp(Alignment.NAME_PROPERTY, thisAlmtName));
				}
			});
		}
		
	}

}
