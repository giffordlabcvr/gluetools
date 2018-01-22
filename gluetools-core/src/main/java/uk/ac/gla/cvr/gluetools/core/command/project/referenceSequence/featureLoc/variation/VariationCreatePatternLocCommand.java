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
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
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
		commandWords={"create","pattern-location"}, 
		docoptUsages={"[-C] <pattern> ( -n <ntStart> <ntEnd> | -c <lcStart> <lcEnd> )"},
		docoptOptions={
				"-C, --noCommit       Don't commit to the database [default: false]",
				"-n, --nucleotide     Set location based on reference sequence nucleotide",
				"-c, --labeledCodon   Set location based on labeled codons"},
		metaTags={CmdMeta.updatesDatabase},
		description="Craete a pattern-location for the variation", 
		furtherHelp="A pattern location is a region of a sequence which should be scanned for a particular pattern. "+
				"The location can be set in different ways. "+ 
				"If the --nucleotide option is used, <ntStart> and <ntEnd> define simply the NT region of the variation's reference sequence, "+
				"using that reference sequence's own coordinates. "+
				"For variations of type AMINO_ACID, the --labeledCodon option may be used. "+
				"In this case labeled codon locations <lcStart>, <lcEnd> are specified using the "+
				"codon-labeling scheme of the variation's feature location.") 
public class VariationCreatePatternLocCommand extends VariationModeCommand<CreateResult> {

	public static final String NO_COMMIT = "noCommit";
	public static final String PATTERN = "pattern";
	public static final String NT_BASED = "nucleotide";
	public static final String NT_START = "ntStart";
	public static final String NT_END = "ntEnd";
	public static final String LC_BASED = "labeledCodon";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";

	
	private Boolean noCommit;
	private String pattern;
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
		pattern = PluginUtils.configureStringProperty(configElem, PATTERN, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
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
	public CreateResult execute(CommandContext cmdContext) {
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
		if(ntStart > ntEnd) {
			throw new VariationException(Code.VARIATION_LOCATION_ENDPOINTS_REVERSED, 
					getRefSeqName(), getFeatureName(), getVariationName(), Integer.toString(ntStart), Integer.toString(ntEnd));
		}
		
		PatternLocation pLoc = GlueDataObject.create(cmdContext, PatternLocation.class, 
				PatternLocation.pkMap(getRefSeqName(), getFeatureName(), getVariationName(), ntStart, ntEnd), false);
		
		pLoc.setRefStart(ntStart);
		pLoc.setRefEnd(ntEnd);
		pLoc.setPattern(pattern);
		pLoc.setVariation(variation);
		
		if(!noCommit) {
			cmdContext.commit();
		}
		return new CreateResult(PatternLocation.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}

	
}
