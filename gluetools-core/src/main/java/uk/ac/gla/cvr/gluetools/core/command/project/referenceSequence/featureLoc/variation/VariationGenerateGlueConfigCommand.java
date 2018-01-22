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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.GenerateConfigCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.result.GlueConfigResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass(
		commandWords={"generate", "glue-config"},
		docoptUsages={"[-C] [-e] (-f <fileName> | -p)"},
		docoptOptions={
				"-C, --noCommit                        Generated commands should not commit",
				"-e, --commitAtEnd                     Add commit command at end",
				"-f <fileName>, --fileName <fileName>  Name of file to output to",
				"-p, --preview                         Preview only"},
		description="Generate GLUE configuration to recreate the variation",
		metaTags={ CmdMeta.consoleOnly, CmdMeta.suppressDocs }
)
public class VariationGenerateGlueConfigCommand extends VariationModeCommand<GlueConfigResult> {
	
	private GenerateConfigCommandDelegate generateConfigCommandDelegate = new GenerateConfigCommandDelegate();
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		generateConfigCommandDelegate.configure(pluginConfigContext, configElem);
	}

	@Override
	public GlueConfigResult execute(CommandContext cmdContext) {
		GlueConfigContext glueConfigContext = new GlueConfigContext(cmdContext, false, 
				generateConfigCommandDelegate.getNoCommit(), 
				generateConfigCommandDelegate.getCommitAtEnd());
		Variation variation = lookupVariation(cmdContext);
		return GlueConfigResult.generateGlueConfigResult(cmdContext, 
				generateConfigCommandDelegate.getPreview(), 
				generateConfigCommandDelegate.getFileName(), 
				variation.generateGlueConfig(glueConfigContext));
	}

	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
		
	}

}
