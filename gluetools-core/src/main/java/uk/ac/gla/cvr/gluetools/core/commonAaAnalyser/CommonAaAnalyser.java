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
package uk.ac.gla.cvr.gluetools.core.commonAaAnalyser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.CodonLabeler;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcidFrequency;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectSetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentAminoAcidFrequencyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.CreateVariationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetMetatagCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


/**
 * 
 * @author joshsinger
 *
 */

@PluginClass(elemName="commonAaAnalyser",
		description="Creates variations to detect amino acid substitutions that are atypical amongst a set of AlignmentMembers")
public class CommonAaAnalyser extends ModulePlugin<CommonAaAnalyser> {

	public static final String MIN_FREQUENCY_PCT = "minFrequencyPct";
	public static final String MIN_SAMPLE_SIZE = "minSampleSize";
	
	private double minFrequencyPct;
	private int minSampleSize;

	private List<VariationFieldSetting> variationFieldSettings;
	

	
	public CommonAaAnalyser() {
		super();
		addSimplePropertyName(MIN_FREQUENCY_PCT);
		addSimplePropertyName(MIN_SAMPLE_SIZE);
		registerModulePluginCmdClass(ShowCommonAasCommand.class);
		registerModulePluginCmdClass(GenerateVariationUncommonAasCommand.class);
		registerModuleDocumentCmdClass(AddVariationFieldSettingCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		minFrequencyPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_FREQUENCY_PCT, false)).orElse(1.0);
		minSampleSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MIN_SAMPLE_SIZE, false)).orElse(30);
		
		variationFieldSettings = PluginFactory.createPlugins(pluginConfigContext, VariationFieldSetting.class, 
				PluginUtils.findConfigElements(configElem, "variationFieldSetting"));

		
	}

	public List<CommonAminoAcids> commonAas(CommandContext cmdContext,
			String alignmentName, String acRefName, String featureName,
			Optional<Expression> whereClause, Boolean recursive) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		int memberCount = AlignmentListMemberCommand.countMembers(cmdContext, alignment, true, whereClause);
		if(memberCount < minSampleSize) {
			return Collections.emptyList();
		}
		List<LabeledAminoAcidFrequency> almtAaFreqs = 
				AlignmentAminoAcidFrequencyCommand
					.alignmentAminoAcidFrequencies(cmdContext, alignmentName, acRefName, featureName, whereClause, recursive, null, null);

		Map<String, CommonAminoAcids> codonLabelToCommonAas = new LinkedHashMap<String, CommonAminoAcids>();
		almtAaFreqs.forEach(almtAaFreq -> {
			if(almtAaFreq.getTotalMembers() < minSampleSize) {
				return;
			}
			if(almtAaFreq.getPctMembers() < minFrequencyPct) {
				return;
			}
			String aa = almtAaFreq.getAminoAcid();
			if(aa.equals("X")) {
				return;
			}
			String codonLabel = almtAaFreq.getLabeledCodon().getCodonLabel();
			CommonAminoAcids commonAas = codonLabelToCommonAas
					.computeIfAbsent(codonLabel, cdnLbl -> new CommonAminoAcids(acRefName, featureName, codonLabel));
			commonAas.getCommonAas().add(aa);
		});
		
		codonLabelToCommonAas.values().forEach(commonAas -> {
			commonAas.getCommonAas().sort(Comparator.naturalOrder());
		});
		
		List<CommonAminoAcids> result = new ArrayList<CommonAminoAcids>(codonLabelToCommonAas.values());
		
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName), false);

		final CodonLabeler codonLabeler = feature.getCodonLabelerModule(cmdContext);
		Collections.sort(result, new Comparator<CommonAminoAcids>(){
			@Override
			public int compare(CommonAminoAcids o1, CommonAminoAcids o2) {
				String codonLabel1 = o1.getCodonLabel();
				String codonLabel2 = o2.getCodonLabel();
				if(codonLabeler != null) {
					return codonLabeler.compareCodonLabels(codonLabel1, codonLabel2);
				} else {
					return Integer.compare(Integer.parseInt(codonLabel1), Integer.parseInt(codonLabel2));
				}
			}
		});
		return result;
		
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
	}


	public static class VariationFieldSetting implements Plugin {

		private String fieldName;
		private String fieldValue;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			Plugin.super.configure(pluginConfigContext, configElem);
			fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
			fieldValue = PluginUtils.configureStringProperty(configElem, "fieldValue", true);
		}
		public String getFieldName() {
			return fieldName;
		}
		public String getFieldValue() {
			return fieldValue;
		}
	}

	public List<Map<String, String>> generateVariationUncommonAas(CommandContext cmdContext, List<CommonAminoAcids> commonAasList) {
		int variationsCreated = 0;
		List<Map<String,String>> variationPkMaps = new ArrayList<Map<String,String>>();
		for(CommonAminoAcids commonAas : commonAasList) {
			String refName = commonAas.getRefName();
			try(ModeCloser referenceMode = cmdContext.pushCommandMode("reference", refName)) {
				String featureName = commonAas.getFeatureName();
				try(ModeCloser fLocMode = cmdContext.pushCommandMode("feature-location", featureName)) {
					String variationName = generateUncommonAaVariationName(commonAas);
					String codonLabel = commonAas.getCodonLabel();
					cmdContext.cmdBuilder(CreateVariationCommand.class)
					.set(CreateVariationCommand.VARIATION_NAME, variationName)
					.set(CreateVariationCommand.NO_COMMIT, Boolean.TRUE)
					.set(CreateVariationCommand.VTYPE, Variation.VariationType.aminoAcidPolymorphism)
					.set(CreateVariationCommand.LC_BASED, Boolean.TRUE)
					.set(CreateVariationCommand.LC_START, codonLabel)
					.set(CreateVariationCommand.LC_END, codonLabel)
					.build().execute(cmdContext);
					variationPkMaps.add(Variation.pkMap(refName, featureName, variationName));
					try(ModeCloser varationMode = cmdContext.pushCommandMode("variation", variationName)) {
						cmdContext.cmdBuilder(VariationSetMetatagCommand.class)
						.set(VariationSetMetatagCommand.NO_COMMIT, Boolean.TRUE)
						.set(VariationSetMetatagCommand.METATAG_NAME, VariationMetatagType.PATTERN.name())
						.set(VariationSetMetatagCommand.METATAG_VALUE, generateUncommonAaVariationRegex(commonAas))
						.build().execute(cmdContext);
						
						cmdContext.cmdBuilder(ConfigurableObjectSetFieldCommand.class)
						.set(ConfigurableObjectSetFieldCommand.FIELD_NAME, Variation.DISPLAY_NAME_PROPERTY)
						.set(ConfigurableObjectSetFieldCommand.FIELD_VALUE, generateUncommonAaVariationDisplayName(commonAas))
						.set(ConfigurableObjectSetFieldCommand.NO_COMMIT, Boolean.TRUE)
						.build().execute(cmdContext);
						
						for(VariationFieldSetting variationFieldSetting: variationFieldSettings) {
							cmdContext.cmdBuilder(ConfigurableObjectSetFieldCommand.class)
							.set(ConfigurableObjectSetFieldCommand.FIELD_NAME, variationFieldSetting.fieldName)
							.set(ConfigurableObjectSetFieldCommand.FIELD_VALUE, variationFieldSetting.fieldValue)
							.set(ConfigurableObjectSetFieldCommand.NO_COMMIT, Boolean.TRUE)
							.build().execute(cmdContext);
						}
					}
				}
			}
			variationsCreated++;
			if(variationsCreated % 500 == 0) {
				log(Level.FINEST, "Created "+variationsCreated+" variations in the project");
				cmdContext.commit();
			}
		}
		log(Level.FINEST, "Created "+variationsCreated+" variations in the project");
		cmdContext.commit();
		return variationPkMaps;
	}

	private String generateUncommonAaVariationDisplayName(CommonAminoAcids commonAas) {
		return "Atypical "+commonAas.getFeatureName()+":"+commonAas.getCodonLabel();
	}

	private String generateUncommonAaVariationRegex(CommonAminoAcids commonAas) {
		return "[^"+String.join("", commonAas.getCommonAas())+"X]";
	}

	private String generateUncommonAaVariationName(CommonAminoAcids commonAas) {
		return "uncommon_aa_"+commonAas.getFeatureName()+":"+commonAas.getCodonLabel();
	}

	
}
