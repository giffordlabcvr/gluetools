package uk.ac.gla.cvr.gluetools.core.commonAaPolymorphisms;

import java.util.ArrayList;
import java.util.Arrays;
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
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcidFrequency;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentAminoAcidFrequencyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.CreateVariationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetLocationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetPatternCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;


/**
 * 
 * @author joshsinger
 *
 */

@PluginClass(elemName="commonAaPolymorphismGenerator")
public class CommonAaPolymorphismGenerator extends ModulePlugin<CommonAaPolymorphismGenerator> {

	private static final String MIN_FREQUENCY_PCT = "minFrequencyPct";
	private static final String MAX_FREQUENCY_PCT = "maxFrequencyPct";
	private static final String MIN_SAMPLE_SIZE = "minSampleSize";
	private double minFrequencyPct;
	private double maxFrequencyPct;
	private int minSampleSize;
	private List<CustomFieldSetting> customFieldSettings;
	
	
	public CommonAaPolymorphismGenerator() {
		super();
		addSimplePropertyName(MIN_FREQUENCY_PCT);
		addSimplePropertyName(MAX_FREQUENCY_PCT);
		addSimplePropertyName(MIN_SAMPLE_SIZE);
		addModulePluginCmdClass(GenerateCommand.class);
		addModuleDocumentCmdClass(AddCustomFieldSettingCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		minFrequencyPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_FREQUENCY_PCT, false)).orElse(1.0);
		maxFrequencyPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MAX_FREQUENCY_PCT, false)).orElse(99.0);
		minSampleSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MAX_FREQUENCY_PCT, false)).orElse(30);
		customFieldSettings = PluginFactory.createPlugins(pluginConfigContext, CustomFieldSetting.class, 
				PluginUtils.findConfigElements(configElem, "customFieldSetting"));
	}

	public List<AaPolymorphism> preview(CommandContext cmdContext,
			Alignment alignment, ReferenceSequence ancConstrainingRef, 
			String featureName,
			Optional<Expression> whereClause, Boolean recursive) {

		Map<AaPolymorphismKey, AaPolymorphism> generated = new LinkedHashMap<AaPolymorphismKey, AaPolymorphism>();
		
		// prioritizedAncestorReferences is a list of reference sequence from the ancConstrainingRef down to the reference of this alignment.
		List<ReferenceSequence> allAncestorReferences = alignment.getAncestorReferences();
		List<ReferenceSequence> prioritizedAncestorReferences = new ArrayList<ReferenceSequence>();
		for(ReferenceSequence refSeq: allAncestorReferences) {
			prioritizedAncestorReferences.add(0, refSeq);
			if(refSeq.getName().equals(ancConstrainingRef.getName())) {
				break;
			}
		}
		// cache of codon->refAA maps for each reference.
		Map<String, Map<String, String>> refToLabeledAas = new LinkedHashMap<String, Map<String, String>>();

		previewAux(cmdContext, alignment, ancConstrainingRef,
				featureName, whereClause, generated,
				prioritizedAncestorReferences, refToLabeledAas);
		
		if(recursive) {
			List<Alignment> childAlignments = alignment.getChildren();
			for(Alignment childAlignment: childAlignments) {
				prioritizedAncestorReferences.add(childAlignment.getConstrainingRef());
				previewAux(cmdContext, childAlignment, ancConstrainingRef,
						featureName, whereClause, generated,
						prioritizedAncestorReferences, refToLabeledAas);
				prioritizedAncestorReferences.remove(prioritizedAncestorReferences.size()-1);
			}
		}
		
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		final CodonLabeler codonLabeler = feature.getCodonLabelerModule(cmdContext);
		ArrayList<AaPolymorphism> result = new ArrayList<AaPolymorphism>(generated.values());
		Collections.sort(result, new Comparator<AaPolymorphism>(){
			@Override
			public int compare(AaPolymorphism o1, AaPolymorphism o2) {
				int comp;
				String codonLabel1 = o1.getCodonLabel();
				String codonLabel2 = o2.getCodonLabel();
				if(codonLabeler != null) {
					comp = codonLabeler.compareCodonLabels(codonLabel1, codonLabel2);
				} else {
					comp = Integer.compare(Integer.parseInt(codonLabel1), Integer.parseInt(codonLabel2));
				}
				if(comp != 0) { return comp; }
				comp = o1.getRefAa().compareTo(o2.getRefAa()); // can't think why these would be different really!
				if(comp != 0) { return comp; }
				return o1.getVariationAa().compareTo(o2.getVariationAa()); 
			}
		});
		super.log(Level.FINEST, "Generated "+generated.size()+" common AA polymorphisms in "+featureName);
		return result;
		
	}

	private void previewAux(CommandContext cmdContext, Alignment alignment,
			ReferenceSequence ancConstrainingRef,
			String featureName,
			Optional<Expression> whereClause,
			Map<AaPolymorphismKey, AaPolymorphism> generated,
			List<ReferenceSequence> prioritizedAncestorReferences,
			Map<String, Map<String, String>> refToLabeledAas) {
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, alignment, true, true, whereClause);
		if(almtMembers.size() < minSampleSize) {
			return;
		}
		String alignmentRefName = alignment.getRefSequence().getName();
		FeatureLocation scannedFeatureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(alignmentRefName, featureName), true);
		if(scannedFeatureLoc == null) {
			return;
		}
		
		List<LabeledAminoAcidFrequency> almtAaFreqs = AlignmentAminoAcidFrequencyCommand.alignmentAminoAcidFrequencies(cmdContext, alignment, alignment.getRefSequence(), scannedFeatureLoc, almtMembers);

		
		for(LabeledAminoAcidFrequency almtAaFreq: almtAaFreqs) {
			if(almtAaFreq.getTotalMembers() >= minSampleSize &&
					almtAaFreq.getPctMembers() >= minFrequencyPct &&
					almtAaFreq.getPctMembers() < maxFrequencyPct) {
				String codonLabel = almtAaFreq.getLabeledAminoAcid().getLabeledCodon().getCodonLabel();
				String variationAa = almtAaFreq.getLabeledAminoAcid().getAminoAcid();
				// if variation has already been generated for some ancestor reference, we don't need a new variation.
				if(prioritizedAncestorReferences.stream()
						.anyMatch(ref -> generated.containsKey(new AaPolymorphismKey(ref.getName(), codonLabel, variationAa)))) {
					continue;
				}
				for(ReferenceSequence refSeq: prioritizedAncestorReferences) {
					String refName = refSeq.getName();
					Map<String, String> codonToRefAa = refToLabeledAas.get(refName);
					if(codonToRefAa == null) {
						FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
								FeatureLocation.pkMap(refName, featureName));
						List<LabeledAminoAcid> featureLocAminoAcids = FeatureLocAminoAcidCommand.featureLocAminoAcids(cmdContext, featureLoc);
						codonToRefAa = new LinkedHashMap<String, String>();
						final Map<String, String> codonToRefAaF = codonToRefAa;
						featureLocAminoAcids.forEach(laa -> codonToRefAaF.put(laa.getLabeledCodon().getCodonLabel(), laa.getAminoAcid()));
						refToLabeledAas.put(refName, codonToRefAa);
					}
					String refAa = codonToRefAa.get(codonLabel);
					if(refAa != null) {
						// "X" just means any amino acid, so don't generate variations from these.
						if(!refAa.equals("X") && !variationAa.equals("X") ) { 
							String variationName = formVariationName(refAa, codonLabel, variationAa);
							generated.put(new AaPolymorphismKey(alignmentRefName, codonLabel, variationAa), 
									new AaPolymorphism(alignmentRefName, featureName, variationName, codonLabel, refAa, variationAa, 
											"Common amino acid polymorphism in "+alignment.getName()));
							if(generated.size() % 500 == 0) {
								super.log(Level.FINEST, "Generated "+generated.size()+" common AA polymorphisms in "+featureName);
							}
							break;
						}
					}
				}
			}
		}
	}

	private String formVariationName(String refAa, String codonLabel, String variationAa) {
		return refAa+codonLabel+variationAa;
	}
	
	
	private static class AaPolymorphismKey {
		private String refName;
		private String codonLabel;
		private String variationAa;
		public AaPolymorphismKey(String refName, String codonLabel,
				String variationAa) {
			super();
			this.refName = refName;
			this.codonLabel = codonLabel;
			this.variationAa = variationAa;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((codonLabel == null) ? 0 : codonLabel.hashCode());
			result = prime * result
					+ ((refName == null) ? 0 : refName.hashCode());
			result = prime * result
					+ ((variationAa == null) ? 0 : variationAa.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AaPolymorphismKey other = (AaPolymorphismKey) obj;
			if (codonLabel == null) {
				if (other.codonLabel != null)
					return false;
			} else if (!codonLabel.equals(other.codonLabel))
				return false;
			if (refName == null) {
				if (other.refName != null)
					return false;
			} else if (!refName.equals(other.refName))
				return false;
			if (variationAa == null) {
				if (other.variationAa != null)
					return false;
			} else if (!variationAa.equals(other.variationAa))
				return false;
			return true;
		}
		
		
	}
	
	public void generate(CommandContext cmdContext, List<AaPolymorphism> aaPolymorphisms) {
		int variationsCreated = 0;
		for(AaPolymorphism aaPolymorphism : aaPolymorphisms) {
			try(ModeCloser referenceMode = cmdContext.pushCommandMode("reference", aaPolymorphism.getRefName())) {
				try(ModeCloser fLocMode = cmdContext.pushCommandMode("feature-location", aaPolymorphism.getFeatureName())) {
					String variationName = aaPolymorphism.getVariationName();
					cmdContext.cmdBuilder(CreateVariationCommand.class)
					.set(CreateVariationCommand.VARIATION_NAME, variationName)
					.set(CreateVariationCommand.NO_COMMIT, Boolean.TRUE)
					.set(CreateVariationCommand.TRANSLATION_TYPE, TranslationFormat.AMINO_ACID.name())
					.set(CreateVariationCommand.DESCRIPTION, aaPolymorphism.getDescription())
					.build().execute(cmdContext);
					try(ModeCloser varationMode = cmdContext.pushCommandMode("variation", variationName)) {
						String codonLabel = aaPolymorphism.getCodonLabel();
						cmdContext.cmdBuilder(VariationSetLocationCommand.class)
						.set(VariationSetLocationCommand.NO_COMMIT, Boolean.TRUE)
						.set(VariationSetLocationCommand.LC_BASED, Boolean.TRUE)
						.set(VariationSetLocationCommand.LC_START, codonLabel)
						.set(VariationSetLocationCommand.LC_END, codonLabel)
						.build().execute(cmdContext);
						cmdContext.cmdBuilder(VariationSetPatternCommand.class)
						.set(VariationSetPatternCommand.NO_COMMIT, Boolean.TRUE)
						.set(VariationSetPatternCommand.REGEX, aaPolymorphism.getRegex())
						.build().execute(cmdContext);
						for(CustomFieldSetting customFieldSetting: customFieldSettings) {
							cmdContext.cmdBuilder(VariationSetFieldCommand.class)
							.set(VariationSetFieldCommand.FIELD_NAME, customFieldSetting.fieldName)
							.set(VariationSetFieldCommand.FIELD_VALUE, customFieldSetting.fieldValue)
							.set(VariationSetFieldCommand.NO_COMMIT, Boolean.TRUE)
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
	}

	
	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		 InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		 for(CustomFieldSetting customFieldSetting: customFieldSettings) {
			 insideProjectMode.getProject().checkCustomFieldNames(ConfigurableTable.variation, Arrays.asList(customFieldSetting.getFieldName()));
		 }
	}


	public static class CustomFieldSetting implements Plugin {

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
	
}