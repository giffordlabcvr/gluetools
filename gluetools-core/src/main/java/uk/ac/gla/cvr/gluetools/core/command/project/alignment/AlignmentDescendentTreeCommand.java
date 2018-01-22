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

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"descendent-tree"},
		docoptUsages={"[-s <sortProperties>]"},
		docoptOptions={
			"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties"},
		furtherHelp=
				"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
				"This is applied when sorting child alignments.\n",
		description="Render the descendents of this alignment as a tree"
	) 
public class AlignmentDescendentTreeCommand extends AlignmentModeCommand<AlignmentDescendentTreeResult> {

	public static final String SORT_PROPERTIES = "sortProperties";
	
	private String sortProperties;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.sortProperties = PluginUtils.configureStringProperty(configElem, SORT_PROPERTIES, false);
	}



	@Override
	public AlignmentDescendentTreeResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		return new AlignmentDescendentTreeResult(cmdContext, alignment, sortProperties);
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}

}
