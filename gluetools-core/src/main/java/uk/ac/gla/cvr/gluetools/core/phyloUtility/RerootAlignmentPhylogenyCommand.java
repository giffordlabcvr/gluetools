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

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeMidpointFinder;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeMidpointResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.PhyloExporter;

@CommandClass(
		commandWords={"reroot-alignment-phylogeny"}, 
		description = "Reroot a phylogenetic tree associated with an alignment.", 
		docoptUsages = { "<alignmentName> <fieldName> (-w <whereClause> [-r] | -m) -o <outputFile> <outputFormat>"},
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>  Specify outgroup alignment members",
				"-r, --removeOutgroup                           Remove outgroup subtree in output",
				"-m, --midpoint                                 Use midpoint rooting",
				"-o <outputFile>, --outputFile <outputFile>     Output file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class RerootAlignmentPhylogenyCommand extends BaseRerootCommand {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String FIELD_NAME = "fieldName";
	public static final String OUTGROUP_WHERE_CLAUSE = "whereClause";
	public static final String REMOVE_OUTGROUP = "removeOutgroup";
	public static final String MIDPOINT = "midpoint";
	
	private Expression outgroupWhereClause;
	private String alignmentName;
	private String fieldName;
	private Boolean removeOutgroup;
	private Boolean midpoint;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		this.outgroupWhereClause = PluginUtils.configureCayenneExpressionProperty(configElem, OUTGROUP_WHERE_CLAUSE, false);
		this.removeOutgroup = PluginUtils.configureBooleanProperty(configElem, REMOVE_OUTGROUP, false);
		this.midpoint = PluginUtils.configureBooleanProperty(configElem, MIDPOINT, false);
		if( (outgroupWhereClause == null && (midpoint == null || !midpoint)) ||
				(outgroupWhereClause != null && (midpoint != null && midpoint))) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or --midpoint must be specified, but not both");
		}
		if(outgroupWhereClause == null && (removeOutgroup != null && removeOutgroup)) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "The --removeOutgroup option may only be used if <whereClause> is specified");
		}
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, PhyloUtility phyloUtility) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		if(alignment.isConstrained()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Command only supports unconstrained alignments at the moment.");
		}
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		project.checkProperty(ConfigurableTable.alignment.name(), fieldName, EnumSet.of(FieldType.VARCHAR, FieldType.CLOB), false);
		PhyloTree phyloTree = 
				PhyloExporter.exportAlignmentPhyloTree(cmdContext, alignment, fieldName, false);
		PhyloBranch rerootBranch = null;
		BigDecimal rerootDistance = null;
		if(outgroupWhereClause != null) {
			rerootBranch = phyloUtility.findOutgroupBranch(cmdContext, alignment, outgroupWhereClause, phyloTree);
			rerootDistance = rerootBranch.getLength().divide(new BigDecimal(2.0));
		} else {
			// midpoint rooting
			PhyloTreeMidpointFinder midpointFinder = new PhyloTreeMidpointFinder();
			PhyloTreeMidpointResult midPointResult = midpointFinder.findMidPoint(phyloTree);
			rerootBranch = midPointResult.getBranch();
			rerootDistance = midPointResult.getRootDistance();
		}
		PhyloTree rerootedTree = phyloUtility.rerootPhylogeny(rerootBranch, rerootDistance);
		if(outgroupWhereClause != null && removeOutgroup) {
			// TODO
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "--removeOutgroup unimplemented at the moment.");
		}
		super.saveRerootedTree(cmdContext, rerootedTree);
		return new OkResult();
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
			registerPathLookup("outputFile", false);
			registerEnumLookup("outputFormat", PhyloFormat.class);
		}
		
	}
	
}
