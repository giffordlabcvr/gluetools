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
package uk.ac.gla.cvr.gluetools.core.phylogenyImporter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylogenyImporter.PhyloImporter.AlignmentPhylogeny;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeReconciler;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"import", "phylogeny"}, 
		description = "Import a phylogeny into an alignment / alignment tree", 
		docoptUsages={"<alignmentName> [-c] [-n] (-w <whereClause> | -a) -i <inputFile> <inputFormat> (-f <fieldName> [-m] | -p) "},
		docoptOptions={
			"-c, --recursive                                Include descendent members",
			"-n, --anyAlignment                             Allow match to any alignment",
			"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		    "-a, --allMembers                               All members",
			"-i <inputFile>, --inputFile <inputFile>        Phylogeny input file",
			"-f <fieldName>, --fieldName <fieldName>        Phylogeny field name",
			"-p, --preview                                  Preview only", 
			"-m, --merge                                    Merge imported with existing"},
		metaTags = {CmdMeta.consoleOnly}, 
		furtherHelp = "Imports a phylogenetic tree from a file, and uses it to populate a custom column of the "+
		"alignment table, for a single unconstrained alignment object, or one or more alignment objects within an alignment tree. "+
		"The leaf nodes of the imported tree are matched up with the alignment members selected by the "+
		"<alignmentName>, --recursive and <whereClause>/--allMembers options. "+
		"There must be exactly one leaf node per selected member, otherwise an error will be thrown. "+
		"Leaf nodes are mapped to alignment member objects by having the format: "+
		"alignment/<almtName>/member/<sourceName>/<sequenceID> "+
		"If the --anyAlignment option is used the alignment mentioned in the name of the incoming leaf\n"+
		"is ignored, instead the member source / seqID must specify a unique member within the selected\n"+
		"alignment members set. "+
		"For alignment trees, the gross phylogenetic structure of the imported tree must match the structure of the alignment tree, "+
		"otherwise an error is thrown. "+
		"The imported tree will be broken up if necessary with the relavent sections annotating each alignment in the alignment tree."+
		"The --merge option can be used to merge imported trees with existing trees in the same field. The two trees must have identical structures "+
		"and values must not clash on any property. The merged tree will contain the union of properties."
)
public class ImportPhylogenyCommand extends ModulePluginCommand<ImportPhylogenyResult, PhyloImporter>{

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String RECURSIVE = "recursive";
	public static final String ANY_ALIGNMENT = "anyAlignment";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";

	public static final String INPUT_FILE = "inputFile";
	public static final String INPUT_FORMAT = "inputFormat";
	public static final String FIELD_NAME = "fieldName";
	public static final String PREVIEW = "preview";
	public static final String MERGE = "merge";
	
	private String alignmentName;
	private Boolean recursive;
	private Boolean anyAlignment;
	private Optional<Expression> whereClause;
	private Boolean allMembers;

	private String inputFile;
	private PhyloFormat inputFormat;
	private String fieldName;
	private Boolean preview;
	private Boolean merge;
	

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		anyAlignment = PluginUtils.configureBooleanProperty(configElem, ANY_ALIGNMENT, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);

		inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
		inputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, INPUT_FORMAT, true);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, false);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, false);
		merge = PluginUtils.configureBooleanProperty(configElem, MERGE, false);

		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if((fieldName != null && preview != null && preview) || 
				(fieldName == null && (preview == null || !preview)) ) {
			usageError2();
		}
		if(merge != null && merge && fieldName == null) {
			usageError3();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or --allMembers must be specified, but not both");
	}

	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fieldName> or --preview must be specified, but not both");
	}

	private void usageError3() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "The --merge option may only be used if <fieldName> is specified");
	}

	@Override
	protected ImportPhylogenyResult execute(CommandContext cmdContext, PhyloImporter phylogenyImporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		
		PhyloTree phyloTree = inputFormat.parse(consoleCmdContext.loadBytes(inputFile));
		List<AlignmentPhylogeny> almtPhylogenies = 
				phylogenyImporter.previewImportPhylogeny(cmdContext, phyloTree, alignmentName, recursive, anyAlignment, whereClause);
		if(fieldName != null) {
			for(AlignmentPhylogeny almtPhylogeny: almtPhylogenies) {
				// save string to field in format based on project setting.
				PhyloTree updatedPhyloTree;
				if(merge) {
					Object existingPhyloTreeObj = almtPhylogeny.getAlignment().readProperty(fieldName);
					if(existingPhyloTreeObj == null) {
						updatedPhyloTree = almtPhylogeny.getPhyloTree();
					} else {
						if(!(existingPhyloTreeObj instanceof String)) {
							throw new CommandException(Code.COMMAND_FAILED_ERROR, "Alignment field '"+fieldName+"' was not a string");
						}
						PhyloTree existingPhyloTree = 
								Alignment.getPhylogenyPhyloFormat(cmdContext).parse(((String) existingPhyloTreeObj).getBytes());
						updatedPhyloTree = mergeTrees(existingPhyloTree, almtPhylogeny.getPhyloTree());
					}
				} else {
					updatedPhyloTree = almtPhylogeny.getPhyloTree();
				}
				
				
				PropertyCommandDelegate.executeSetField(cmdContext, project, ConfigurableTable.alignment.name(), 
						almtPhylogeny.getAlignment(), fieldName, 
						new String(Alignment.getPhylogenyPhyloFormat(cmdContext).generate(updatedPhyloTree)), true);
				
			}
			cmdContext.commit();
		}
		return new ImportPhylogenyResult(almtPhylogenies);
	}


	private PhyloTree mergeTrees(PhyloTree phyloTree1, PhyloTree phyloTree2) {
		PhyloTreeReconciler reconciler = new PhyloTreeReconciler(phyloTree1);
		phyloTree2.accept(reconciler);
		reconciler.getSuppliedToVisited().forEach((obj1, obj2) -> {
			Map<String, Object> userData1 = obj1.ensureUserData();
			Map<String, Object> userData2 = obj2.ensureUserData();
			
			userData1.forEach( (key,val1) -> {
				Object val2 = userData2.get(key);
				if(val2 == null) {
					userData2.put(key, val1);
				} else if(val2.equals(val1)) {
					// do nothing 
				} else {
					throw new ImportPhylogenyException(ImportPhylogenyException.Code.TREE_PROPERTY_MISMATCH, "Missmatched values for property '"+key+"' on tree objects of type "+obj1.getClass().getSimpleName());
				}
			} );
			
		});
		return phyloTree2;
	}


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerPathLookup("inputFile", false);
			registerEnumLookup("inputFormat", PhyloFormat.class);
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
					List<String> modifiableFieldNames = project.getModifiableFieldNames(ConfigurableTable.alignment.name());
					return modifiableFieldNames.stream().map(n -> new CompletionSuggestion(n, true)).collect(Collectors.toList());
				}
			});
		}
	}
	
}
