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
package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.PhyloExporter;

@CommandClass(
		commandWords={"read-alignment-phylogeny"}, 
		description = "Return a phylogenetic tree associated with an alignment.", 
		docoptUsages = { "<alignmentName> <fieldName>"},
		docoptOptions = { 
		},
		furtherHelp = "Tree returned as GLUE command document",
		metaTags = {}	
)
public class ReadAlignmentPhylogenyCommand extends PhyloUtilityCommand<PhyloTreeResult> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String FIELD_NAME = "fieldName";
	
	private String alignmentName;
	private String fieldName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
	}

	@Override
	protected PhyloTreeResult execute(CommandContext cmdContext, PhyloUtility phyloUtility) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		project.checkProperty(ConfigurableTable.alignment.name(), fieldName, EnumSet.of(FieldType.VARCHAR, FieldType.CLOB), false);
		PhyloTree phyloTree = 
				PhyloExporter.exportAlignmentPhyloTree(cmdContext, alignment, fieldName, false);
		return new PhyloTreeResult(phyloTree);
	}


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("fieldName", new AdvancedCmdCompleter.VariableInstantiator() {
					@Override
					public List<CompletionSuggestion> instantiate(
							ConsoleCommandContext cmdContext,
							@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
							String prefix) {
						InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
						Project project = insideProjectMode.getProject();
						List<String> listableFieldNames = project.getModifiableFieldNames(ConfigurableTable.alignment.name());
						return listableFieldNames.stream().map(n -> new CompletionSuggestion(n, true)).collect(Collectors.toList());
					}
			});
		}
		
	}
	
}
