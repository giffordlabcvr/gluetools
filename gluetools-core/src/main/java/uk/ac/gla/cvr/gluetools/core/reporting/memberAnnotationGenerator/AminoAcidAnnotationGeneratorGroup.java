package uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAminoAcidColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleLqaaAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.SingleMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="aminoAcidAnnotationGeneratorGroup")
public class AminoAcidAnnotationGeneratorGroup extends AnnotationGeneratorGroup {

	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";
	
	private String relRefName;
	private String featureName;
	private String lcStartName;
	private String lcEndName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.lcStartName = PluginUtils.configureStringProperty(configElem, LC_START, true);
		this.lcEndName = PluginUtils.configureStringProperty(configElem, LC_END, true);
	}
	
	@Override
	public List<String> getAnnotationNames(CommandContext cmdContext) {
		// check reference exists
		GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relRefName));
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName), false);
		featureLoc.getFeature().checkCodesAminoAcids();
		
		LabeledCodon lcStart = featureLoc.getLabeledCodon(cmdContext, lcStartName);
		LabeledCodon lcEnd = featureLoc.getLabeledCodon(cmdContext, lcEndName);
		
		List<LabeledCodon> lcs = featureLoc.getLabeledCodons(cmdContext, lcStart, lcEnd);
		
		return lcs.stream().map(lc -> getAnnotationNamePrefix()+getAnnotationNamePrefix()+lc.getCodonLabel()).collect(Collectors.toList());		
		
	}

	@Override
	public Map<String, String> generateAnnotations(CommandContext cmdContext, AlignmentMember almtMember) {
		Alignment alignment = almtMember.getAlignment();
		alignment.getRelatedRef(cmdContext, relRefName);
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName), false);
		featureLoc.getFeature().checkCodesAminoAcids();
		
		SimpleAminoAcidColumnsSelector alignmentColumnsSelector 
			= new SimpleAminoAcidColumnsSelector(relRefName, featureName, lcStartName, lcEndName);
		
		SimpleLqaaAlmtRowConsumer simpleLqaaAlmtRowConsumer = new SimpleLqaaAlmtRowConsumer();
		alignmentColumnsSelector.generateLqaaAlignmentRows(cmdContext, false, 
				new SingleMemberSupplier(almtMember), simpleLqaaAlmtRowConsumer);

		Map<String, String> results = new LinkedHashMap<String, String>();
		LabeledCodon lcStart = featureLoc.getLabeledCodon(cmdContext, lcStartName);
		LabeledCodon lcEnd = featureLoc.getLabeledCodon(cmdContext, lcEndName);
		List<LabeledCodon> lcs = featureLoc.getLabeledCodons(cmdContext, lcStart, lcEnd);
		lcs.forEach(lc -> results.put(getAnnotationNamePrefix()+lc.getCodonLabel(), "-"));
		
		List<LabeledQueryAminoAcid> lqaas = simpleLqaaAlmtRowConsumer.getResult();
		lqaas.forEach(lqaa -> results.put(getAnnotationNamePrefix()+lqaa.getLabeledAminoAcid().getLabeledCodon().getCodonLabel(), lqaa.getLabeledAminoAcid().getAminoAcid()));
		
		return results;
	}

}
