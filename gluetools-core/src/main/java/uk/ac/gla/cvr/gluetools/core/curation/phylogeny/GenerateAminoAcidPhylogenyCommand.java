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

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAminoAcidAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAminoAcidColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.utils.fasta.ProteinSequence;

public abstract class GenerateAminoAcidPhylogenyCommand<P extends PhylogenyGenerator<P>> extends GeneratePhylogenyCommand<P> {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		
		if(getFeatureName() == null && getSelectorName() == null) {
			usageError6();
		}

	}

	private void usageError6() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <selectorName> or both <relRefName> and <featureName> must be specified");
	}

	private IAminoAcidAlignmentColumnsSelector resolveSelector(CommandContext cmdContext) {
		IAminoAcidAlignmentColumnsSelector alignmentColumnsSelector;
		
		if(getSelectorName() != null) {
			alignmentColumnsSelector = Module.resolveModulePlugin(cmdContext, AlignmentColumnsSelector.class, getSelectorName());
		} else if(getRelRefName() != null && getFeatureName() != null) {
			alignmentColumnsSelector = new SimpleAminoAcidColumnsSelector(getRelRefName(), getFeatureName(), null, null);
		} else {
			alignmentColumnsSelector = null;
		}
		return alignmentColumnsSelector;
	}
	
	
	@Override
	protected final OkResult execute(CommandContext cmdContext, P modulePlugin) {
		IAminoAcidAlignmentColumnsSelector alignmentColumnsSelector = resolveSelector(cmdContext);
		QueryMemberSupplier queryMemberSupplier = resolveQueryMemberSupplier();
		
		Map<Map<String, String>, ProteinSequence> memberAminoAcidAlignment = 
				alignmentColumnsSelector.generateAlignmentMap(cmdContext, false, queryMemberSupplier);
		
		PhyloTree phyloTree = generateAminoAcidPhylogeny(cmdContext, modulePlugin, memberAminoAcidAlignment);

		saveTree(cmdContext, phyloTree);

		return new OkResult();
	}

	protected abstract PhyloTree generateAminoAcidPhylogeny(CommandContext cmdContext, P modulePlugin, Map<Map<String, String>, ProteinSequence> memberAminoAcidAlignment);


}
