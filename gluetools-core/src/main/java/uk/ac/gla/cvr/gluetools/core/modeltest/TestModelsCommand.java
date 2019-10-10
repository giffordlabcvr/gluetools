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
package uk.ac.gla.cvr.gluetools.core.modeltest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleNucleotideColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
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
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@CommandClass(
		commandWords={"test", "models"}, 
		description = "Run JModelTest on a nucleotide alignment to compare substitution models", 
		docoptUsages={"<alignmentName> [-s <selectorName> | -r <relRefName> -f <featureName>] [-c] (-w <whereClause> | -a) [-d <dataDir>]"},
		docoptOptions={
				"-s <selectorName>, --selectorName <selectorName>  Column selector module name",
				"-r <relRefName>, --relRefName <relRefName>        Related reference",
				"-f <featureName>, --featureName <featureName>     Restrict to a given feature",
				"-c, --recursive                                   Include descendent members",
				"-w <whereClause>, --whereClause <whereClause>     Qualify members",
				"-a, --allMembers                                  All members",
				"-d <dataDir>, --dataDir <dataDir>                 Save algorithmic data in this directory"},
		metaTags = { CmdMeta.consoleOnly },
		furtherHelp="If supplied, <dataDir> must either not exist or be an empty directory.")
public class TestModelsCommand extends ModulePluginCommand<TestModelsResult, ModelTester> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String SELECTOR_NAME = "selectorName";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String DATA_DIR = "dataDir";

	private String alignmentName;
	private String selectorName;
	private String relRefName;
	private String featureName;
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Boolean allMembers;

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
		dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
		selectorName = PluginUtils.configureStringProperty(configElem, SELECTOR_NAME, false);


		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if(selectorName != null && ( relRefName != null || featureName != null )) {
			usageError1a();
		}
		if(relRefName != null && featureName == null || relRefName == null && featureName != null) {
			usageError2();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}
	private void usageError1a() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If <selectorName> is used then <relRefName> and <featureName> may not be used");
	}
	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <relRefName> and <featureName> must be specified or neither");
	}

	@Override
	protected TestModelsResult execute(CommandContext cmdContext, ModelTester modelTester) {
		IAlignmentColumnsSelector alignmentColumnsSelector;
		if(selectorName != null) {
			alignmentColumnsSelector = Module.resolveModulePlugin(cmdContext, AlignmentColumnsSelector.class, selectorName);
		} else if(relRefName != null && featureName != null) {
			alignmentColumnsSelector = new SimpleNucleotideColumnsSelector(relRefName, featureName, null, null);
		} else {
			alignmentColumnsSelector = null;
		}
		
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(this.alignmentName, this.recursive, this.whereClause);

		Map<Map<String, String>, DNASequence> memberNucleotideAlignment = 
				FastaAlignmentExporter.exportAlignment(cmdContext, alignmentColumnsSelector,  
				false, queryMemberSupplier);

		TestModelsResult testModelsResult = modelTester.testModels(cmdContext, memberNucleotideAlignment, dataDir);
		return testModelsResult;
	}


	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("relRefName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String alignmentName = (String) bindings.get("alignmentName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), true);
					if(alignment != null) {
						return(alignment.getRelatedRefs()
								.stream()
								.map(ref -> new CompletionSuggestion(ref.getName(), true)))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String relRefName = (String) bindings.get("relRefName");
					ReferenceSequence acRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relRefName), true);
					if(acRef != null) {
						return(acRef.getFeatureLocations()
								.stream()
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true)))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
			registerPathLookup("dataDir", true);
		}
	}


	
}
