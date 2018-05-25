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
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation.VariationType;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"create","variation"}, 
		docoptUsages={"[-C] <variationName> -t <vtype> [-d <description>] [ -n <ntStart> <ntEnd> | -c <lcStart> <lcEnd> ]"},
		docoptOptions={
				"-C, --noCommit                                 Don't commit to the database [default: false]",
				"-t <vtype>, --vtype <vtype>			        Variation type",
				"-d <description>, --description <description>  Variation description",
				"-n, --nucleotide                               Set location based on reference NT",
				"-c, --labeledCodon                             Set location based on labeled codons"
		},
		metaTags={CmdMeta.updatesDatabase},
		description="Create a new feature variation", 
		furtherHelp="A variation is a known motif which may occur in a sequence aligned to a reference. "+
		"The <type> of the variation defines what kind of variation is scanned for. "+
		"The location can be set in different ways. "+ 
		"If the variation is one of the nucleotideTypes,<ntStart> and <ntEnd> define simply the NT region "+
		"on the variation's reference sequence, "+
		"using that reference sequence's own coordinates. "+
		"For aminoAcid-type variations, the --labeledCodon option may be used. "+
		"In this case labeled codon locations <lcStart>, <lcEnd> are specified using the "+
		"codon-labeling scheme of the variation's feature location.") 
public class CreateVariationCommand extends FeatureLocModeCommand<CreateResult> {

	public static final String NO_COMMIT = "noCommit";
	public static final String VARIATION_NAME = "variationName";
	public static final String DESCRIPTION = "description";
	public static final String VTYPE = "vtype";
	public static final String NT_BASED = "nucleotide";
	public static final String NT_START = "ntStart";
	public static final String NT_END = "ntEnd";
	public static final String LC_BASED = "labeledCodon";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";

	
	
	private Boolean noCommit;
	private String variationName;
	private Optional<String> description;
	private Variation.VariationType vtype;
	private Integer ntStart;
	private Integer ntEnd;
	private Boolean nucleotideBased;
	private String lcStart;
	private String lcEnd;
	private Boolean labeledCodonBased;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
		variationName = PluginUtils.configureStringProperty(configElem, VARIATION_NAME, true);
		vtype = PluginUtils.configureEnumProperty(Variation.VariationType.class, configElem, VTYPE, true);
		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DESCRIPTION, false));
		ntStart = PluginUtils.configureIntProperty(configElem, NT_START, false);
		ntEnd = PluginUtils.configureIntProperty(configElem, NT_END, false);
		nucleotideBased = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, NT_BASED, false)).orElse(false);
		lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		labeledCodonBased = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, LC_BASED, false)).orElse(false);
		if(vtype == VariationType.conjunction) {
			if(nucleotideBased || labeledCodonBased || ntStart != null || ntEnd != null || lcStart != null || lcEnd != null) {
				throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Variation location is not used for conjunctions.");
			}
		} else {
			if(!( 
					(nucleotideBased && !labeledCodonBased && ntStart != null && ntEnd != null && lcStart == null && lcEnd == null) || 
					(!nucleotideBased && labeledCodonBased && ntStart == null && ntEnd == null && lcStart != null && lcEnd != null) ) ) {
				throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Variation location must be defined, either nucleotide or labeled-codon based.");
			}
		}

	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		FeatureLocation featureLoc = lookupFeatureLoc(cmdContext);
		
		Variation variation = GlueDataObject.create(cmdContext, 
				Variation.class, Variation.pkMap(
						featureLoc.getReferenceSequence().getName(), 
						featureLoc.getFeature().getName(), variationName), false);
		variation.setFeatureLoc(featureLoc);
		variation.setType(vtype.name());
		
		if(vtype != VariationType.conjunction) { // conjunction does not require a location.
			if(labeledCodonBased) {
				if(!featureLoc.getFeature().codesAminoAcids()) {
					throw new VariationException(Code.VARIATION_CODON_LOCATION_CAN_NOT_BE_USED_FOR_NUCLEOTIDE_VARIATIONS, 
							getRefSeqName(), getFeatureName(), variationName);
				}

				Map<String, LabeledCodon> labelToLabeledCodon = featureLoc.getLabelToLabeledCodon(cmdContext);
				LabeledCodon startLabeledCodon = labelToLabeledCodon.get(lcStart);
				if(startLabeledCodon == null) {
					throw new VariationException(Code.AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE, 
							getRefSeqName(), getFeatureName(), variationName, lcStart, 
							featureLoc.getFirstLabeledCodon(cmdContext).getCodonLabel(), 
							featureLoc.getLastLabeledCodon(cmdContext).getCodonLabel());
				}
				ntStart = startLabeledCodon.getNtStart();
				LabeledCodon endLabeledCodon = labelToLabeledCodon.get(lcEnd);
				if(endLabeledCodon == null) {
					throw new VariationException(Code.AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE, 
							getRefSeqName(), getFeatureName(), variationName, lcEnd, 
							featureLoc.getFirstLabeledCodon(cmdContext).getCodonLabel(), 
							featureLoc.getLastLabeledCodon(cmdContext).getCodonLabel());
				}
				ntEnd = endLabeledCodon.getNtStart()+2;
			}
			if(ntStart > ntEnd) {
				throw new VariationException(Code.VARIATION_LOCATION_ENDPOINTS_REVERSED, 
						getRefSeqName(), getFeatureName(), variationName, Integer.toString(ntStart), Integer.toString(ntEnd));
			}
			variation.setRefStart(ntStart);
			variation.setRefEnd(ntEnd);
		}		

		description.ifPresent(d -> variation.setDescription(d));
		if(noCommit) {
			cmdContext.cacheUncommitted(variation);
		} else {
			cmdContext.commit();
		}
		return new CreateResult(Variation.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerEnumLookup("vtype", Variation.VariationType.class);
		}
	}

}
