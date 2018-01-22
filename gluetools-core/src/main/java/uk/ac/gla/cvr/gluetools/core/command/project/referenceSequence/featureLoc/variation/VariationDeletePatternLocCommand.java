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

import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;

@CommandClass( 
		commandWords={"delete", "pattern-location"}, 
		docoptUsages={"(-n <ntStart> <ntEnd> | -c <lcStart> <lcEnd> )"},
		docoptOptions={
				"-n, --nucleotide     Location based on reference sequence nucleotide",
				"-c, --labeledCodon   Location based on labeled codons"},
		metaTags={CmdMeta.updatesDatabase},
		description="Delete a pattern-location") 
public class VariationDeletePatternLocCommand extends VariationModeCommand<DeleteResult> {

	public static final String NT_BASED = "nucleotide";
	public static final String NT_START = "ntStart";
	public static final String NT_END = "ntEnd";
	public static final String LC_BASED = "labeledCodon";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";

	
	private Integer ntStart;
	private Integer ntEnd;
	private Boolean nucleotideBased;
	private String lcStart;
	private String lcEnd;
	private Boolean labeledCodonBased;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		ntStart = PluginUtils.configureIntProperty(configElem, NT_START, false);
		ntEnd = PluginUtils.configureIntProperty(configElem, NT_END, false);
		nucleotideBased = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, NT_BASED, false)).orElse(false);
		lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		labeledCodonBased = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, LC_BASED, false)).orElse(false);
		if(!( 
			(nucleotideBased && !labeledCodonBased && ntStart != null && ntEnd != null && lcStart == null && lcEnd == null) || 
			(!nucleotideBased && labeledCodonBased && ntStart == null && ntEnd == null && lcStart != null && lcEnd != null) ) ) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Variation location must either be nucleotide or labeled-codon based.");
		}
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		FeatureLocation featureLoc = lookupFeatureLoc(cmdContext);
		Variation variation = lookupVariation(cmdContext);
		TranslationFormat translationFormat = variation.getTranslationFormat();
		
		if(labeledCodonBased) {
			if(translationFormat != TranslationFormat.AMINO_ACID) {
				throw new VariationException(Code.VARIATION_CODON_LOCATION_CAN_NOT_BE_USED_FOR_NUCLEOTIDE_VARIATIONS, 
						getRefSeqName(), getFeatureName(), getVariationName());
			}
			
			Map<String, LabeledCodon> labelToLabeledCodon = featureLoc.getLabelToLabeledCodon(cmdContext);
			LabeledCodon startLabeledCodon = labelToLabeledCodon.get(lcStart);
			if(startLabeledCodon == null) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE, 
						getRefSeqName(), getFeatureName(), getVariationName(), lcStart, 
						featureLoc.getFirstLabeledCodon(cmdContext).getCodonLabel(), 
						featureLoc.getLastLabeledCodon(cmdContext).getCodonLabel());
			}
			ntStart = startLabeledCodon.getNtStart();
			LabeledCodon endLabeledCodon = labelToLabeledCodon.get(lcEnd);
			if(endLabeledCodon == null) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE, 
						getRefSeqName(), getFeatureName(), getVariationName(), lcEnd, 
						featureLoc.getFirstLabeledCodon(cmdContext).getCodonLabel(), 
						featureLoc.getLastLabeledCodon(cmdContext).getCodonLabel());
			}
			ntEnd = endLabeledCodon.getNtStart()+2;
		}
		
		DeleteResult result = GlueDataObject.delete(cmdContext, PatternLocation.class, 
				PatternLocation.pkMap(getRefSeqName(), getFeatureName(), getVariationName(), ntStart, ntEnd), true);
		
		cmdContext.commit();
		return result;
	}

	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}

}
