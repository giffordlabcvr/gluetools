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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

@CommandClass(
		commandWords={"count", "amino-acid"}, 
		description = "Count occurrences of specific amino-acid value in the feature location", 
		docoptUsages = { "<aminoAcid>" },
		docoptOptions = { },
		metaTags = {}	
)
public class FeatureLocCountAminoAcidCommand extends FeatureLocBaseAminoAcidCommand<FeatureLocCountAminoAcidResult> {

	public static final String AMINO_ACID = "aminoAcid";

	private char aminoAcid;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.aminoAcid = PluginUtils.configureCharProperty(configElem, AMINO_ACID, true);
		if(!TranslationUtils.isAminoAcid(aminoAcid)) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Character "+new String(new char[]{aminoAcid})+" is not an amino acid");
		}
	}
	
	@Override
	public FeatureLocCountAminoAcidResult execute(CommandContext cmdContext) {
		FeatureLocation featureLoc = lookupFeatureLoc(cmdContext);
		List<LabeledAminoAcid> labeledAminoAcids = featureLocAminoAcids(cmdContext, featureLoc);
		int count = 0;
		for(LabeledAminoAcid laa : labeledAminoAcids) {
			if(laa.getAminoAcid().charAt(0) == aminoAcid) {
				count++;
			}
		}
		return new FeatureLocCountAminoAcidResult(new String(new char[]{aminoAcid}), count);
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {}

	
}
