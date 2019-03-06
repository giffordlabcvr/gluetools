package uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleNucleotideColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleStringAlmtRowConsumer;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.SingleMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="nucleotideAnnotationGenerator")
public class NucleotideAnnotationGenerator extends MemberAnnotationGenerator {

	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";
	public static final String NT_START = "ntStart";
	public static final String NT_END = "ntEnd";
	
	private String relRefName;
	private String featureName;
	private String lcStartName;
	private String lcEndName;
	private Integer ntStart;
	private Integer ntEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.lcStartName = PluginUtils.configureStringProperty(configElem, LC_START, false);
		this.lcEndName = PluginUtils.configureStringProperty(configElem, LC_END, false);
		this.ntStart = PluginUtils.configureIntProperty(configElem, NT_START, false);
		this.ntEnd = PluginUtils.configureIntProperty(configElem, NT_END, false);
		
		if(! (
				(this.lcStartName != null && lcEndName != null && this.ntStart == null && ntEnd == null) ||
				(this.lcStartName == null && lcEndName == null && this.ntStart != null && ntEnd != null)
			) ) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "For nucleotideAnnotationGenerator, either lcStart and lcEnd or ntStart and ntEnd must be defined");
		}
	}

	@Override
	public String renderAnnotation(CommandContext cmdContext, AlignmentMember almtMember) {
		Alignment alignment = almtMember.getAlignment();
		alignment.getRelatedRef(cmdContext, relRefName);
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName), false);
		
		Integer ntStart = this.ntStart;
		Integer ntEnd = this.ntEnd;
		if(ntStart == null && ntEnd == null) {
			featureLoc.getFeature().checkCodesAminoAcids();
			ntStart = featureLoc.getLabeledCodon(cmdContext, this.lcStartName).getNtStart();
			ntEnd = featureLoc.getLabeledCodon(cmdContext, this.lcEndName).getNtEnd();
		}
		
		SimpleNucleotideColumnsSelector alignmentColumnsSelector 
			= new SimpleNucleotideColumnsSelector(relRefName, featureName, ntStart, ntEnd);
		
		SimpleStringAlmtRowConsumer simpleStringAlmtRowConsumer = new SimpleStringAlmtRowConsumer();
		
		FastaAlignmentExporter.exportAlignment(cmdContext, alignmentColumnsSelector, false, new SingleMemberSupplier(almtMember), simpleStringAlmtRowConsumer);
		
		return simpleStringAlmtRowConsumer.getResult();
	}

}
