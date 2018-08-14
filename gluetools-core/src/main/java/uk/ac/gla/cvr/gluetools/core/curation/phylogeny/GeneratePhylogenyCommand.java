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
package uk.ac.gla.cvr.gluetools.core.curation.phylogeny;

import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class GeneratePhylogenyCommand<P extends PhylogenyGenerator<P>> extends ModulePluginCommand<OkResult, P> {

	public static final String SELECTOR_NAME = "selectorName";
	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String OUTPUT_FORMAT = "outputFormat";
	
	private String alignmentName;
	private String relRefName;
	private String featureName;
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private String selectorName;

	private String outputFile;
	private PhyloFormat outputFormat;
	
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

		outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
		outputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, OUTPUT_FORMAT, true);

		if(!whereClause.isPresent() && !allMembers || whereClause.isPresent() && allMembers) {
			usageError1();
		}
		if(selectorName != null && (relRefName != null || featureName != null)) {
			usageError1a();
		}
		if(relRefName != null && featureName == null || relRefName == null && featureName != null) {
			usageError2();
		}
		if(this.outputFormat != null && this.outputFile == null) {
			usageError5();
		}
	}

	private void usageError1() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
	}
	private void usageError1a() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "If <selectorName> is used then neither <relRefName> nor <featureName> may be specified");
	}
	private void usageError2() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either both <relRefName> and <featureName> must be specified or neither");
	}
	private void usageError5() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <outputFormat> argument may only be used if <outputFile> is specified");
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

	protected void saveTree(CommandContext cmdContext, PhyloTree phyloTree) {
		// save bytes to file in specified format.
		ConsoleCommandContext consoleCmdContext = ((ConsoleCommandContext) cmdContext);
		consoleCmdContext.saveBytes(outputFile, outputFormat.generate(phyloTree));
	}

	protected QueryMemberSupplier resolveQueryMemberSupplier() {
		QueryMemberSupplier queryMemberSupplier = new QueryMemberSupplier(this.alignmentName, this.recursive, this.whereClause);
		return queryMemberSupplier;
	}

	public static class PhylogenyCommandCompleter extends FastaAlignmentExportCommandDelegate.ExportCompleter {

		public PhylogenyCommandCompleter() {
			super();
			registerPathLookup("outputFile", false);
			registerEnumLookup("outputFormat", PhyloFormat.class);
		}

	}



	
}
