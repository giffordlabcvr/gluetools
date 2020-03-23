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
package uk.ac.gla.cvr.gluetools.programs.cdhit;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleNucleotideColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@CommandClass( 
		commandWords={"generate-clusters", "alignment"}, 
		docoptUsages={"<alignmentName> [-s <selectorName> | -r <relRefName> -f <featureName>] [-c] (-w <whereClause> | -a) [-d <dataDir>]"},
		docoptOptions={
				"-s <selectorName>, --selectorName <selectorName>  Column selector module name",
			"-r <relRefName>, --relRefName <relRefName>            Related reference",
			"-f <featureName>, --featureName <featureName>         Restrict to a given feature",
			"-c, --recursive                                       Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>         Qualify members",
		    "-a, --allMembers                                      All members",
			"-d <dataDir>, --dataDir <dataDir>                     Save algorithmic data in this directory"},
		metaTags = { CmdMeta.consoleOnly },
		description="Generate CD-HIT EST clusters using a nucleotide alignment", 
		furtherHelp="If supplied, <dataDir> must either not exist or be an empty directory.") 
public class CdHitEstGenerateClustersAlignmentCommand extends ModulePluginCommand<CdHitEstGenerateClustersAlignmentResult, CdHitEstRunner> {

	public final static String DATA_DIR = "dataDir";
	
	public static final String SELECTOR_NAME = "selectorName";
	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	
	private String alignmentName;
	private String relRefName;
	private String featureName;
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private String selectorName;
	private String dataDir;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, false);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, false);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		selectorName = PluginUtils.configureStringProperty(configElem, SELECTOR_NAME, false);
		dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);

		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if(selectorName != null && (relRefName != null || featureName != null)) {
			usageError1a();
		}
		if(relRefName != null && featureName == null || relRefName == null && featureName != null) {
			usageError2();
		}
	}

	private void usageError1() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}
	private void usageError1a() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "If <selectorName> is used then neither <relRefName> nor <featureName> may be specified");
	}
	private void usageError2() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either both <relRefName> and <featureName> must be specified or neither");
	}
	
	protected String getFeatureName() {
		return featureName;
	}

	protected String getSelectorName() {
		return selectorName;
	}

	protected String getRelRefName() {
		return relRefName;
	}


	protected QueryMemberSupplier resolveQueryMemberSupplier() {
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(this.alignmentName, this.recursive, this.whereClause);
		return queryMemberSupplier;
	}
	
	@Override
	protected final CdHitEstGenerateClustersAlignmentResult execute(CommandContext cmdContext, CdHitEstRunner cdHitEstRunner) {
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);
		IAlignmentColumnsSelector alignmentColumnsSelector = resolveSelector(cmdContext);
		QueryMemberSupplier queryMemberSupplier = resolveQueryMemberSupplier();
		
		Map<Map<String, String>, DNASequence> memberNucleotideAlignment = 
				FastaAlignmentExporter.exportAlignment(cmdContext, alignmentColumnsSelector, false, queryMemberSupplier);
		
		Map<String, DNASequence> cdHitAlignment = new LinkedHashMap<String, DNASequence>();
		
		memberNucleotideAlignment.forEach( (pkMap, seq) -> {
			cdHitAlignment.put( Project.pkMapToTargetPath(ConfigurableTable.alignment_member.getModePath(), pkMap), seq);
		});
		CdHitResult cdHitResult = cdHitEstRunner.executeCdHitEst(cmdContext, cdHitAlignment, dataDirFile);
		List<CdHitEstGenerateClustersAlignmentResultRow> resultRows = new ArrayList<CdHitEstGenerateClustersAlignmentResultRow>();
		
		for(CdHitCluster cluster: cdHitResult.getClusters()) {
			int clusterNumber = cluster.getClusterNumber();
			Map<String, String> repPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, cluster.getRepresentativeSeqId());
			resultRows.add(new CdHitEstGenerateClustersAlignmentResultRow(repPkMap, clusterNumber, true));
			for(String otherSeqId: cluster.getOtherSeqIds()) {
				Map<String, String> otherPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, otherSeqId);
				resultRows.add(new CdHitEstGenerateClustersAlignmentResultRow(otherPkMap, clusterNumber, false));
			}
		}
		return new CdHitEstGenerateClustersAlignmentResult(resultRows);
	}

	
	private IAlignmentColumnsSelector resolveSelector(CommandContext cmdContext) {
		IAlignmentColumnsSelector alignmentColumnsSelector;
		
		if(getSelectorName() != null) {
			alignmentColumnsSelector = Module.resolveModulePlugin(cmdContext, AlignmentColumnsSelector.class, getSelectorName());
		} else if(getRelRefName() != null && getFeatureName() != null) {
			alignmentColumnsSelector = new SimpleNucleotideColumnsSelector(getRelRefName(), getFeatureName(), null, null);
		} else {
			alignmentColumnsSelector = null;
		}
		return alignmentColumnsSelector;
	}

	

	@CompleterClass
	public static final class Completer extends FastaAlignmentExportCommandDelegate.ExportCompleter {
		public Completer() {
			super();
			registerPathLookup("dataDir", true);
		}
		
	}

}
