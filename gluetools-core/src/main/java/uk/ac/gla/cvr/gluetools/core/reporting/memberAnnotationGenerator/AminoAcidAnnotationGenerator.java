package uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAminoAcidColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleStringAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.SingleMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="aminoAcidAnnotationGenerator")
public class AminoAcidAnnotationGenerator extends MemberAnnotationGenerator {

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

	
	public AminoAcidAnnotationGenerator() {
		super();
	}

	public AminoAcidAnnotationGenerator(String annotationName, String relRefName, String featureName, 
			String lcStartName, String lcEndName) {
		super(annotationName);
		this.relRefName = relRefName;
		this.featureName = featureName;
		this.lcStartName = lcStartName;
		this.lcEndName = lcEndName;
	}


	@Override
	public String renderAnnotation(CommandContext cmdContext, AlignmentMember almtMember) {
		Alignment alignment = almtMember.getAlignment();
		alignment.getRelatedRef(cmdContext, relRefName);
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName), false);
		featureLoc.getFeature().checkCodesAminoAcids();
		
		SimpleAminoAcidColumnsSelector alignmentColumnsSelector 
			= new SimpleAminoAcidColumnsSelector(relRefName, featureName, lcStartName, lcEndName);
		
		SimpleStringAlmtRowConsumer simpleStringAlmtRowConsumer = new SimpleStringAlmtRowConsumer();
		alignmentColumnsSelector.generateStringAlignmentRows(cmdContext, false, 
				new SingleMemberSupplier(almtMember), simpleStringAlmtRowConsumer);

		return simpleStringAlmtRowConsumer.getResult();
	}

}
