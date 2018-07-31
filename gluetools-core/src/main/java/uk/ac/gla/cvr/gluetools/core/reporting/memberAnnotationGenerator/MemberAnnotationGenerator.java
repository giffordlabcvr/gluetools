package uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class MemberAnnotationGenerator implements Plugin {

	public static final String ANNOTATION_NAME = "annotationName";
	
	private String annotationName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.annotationName = PluginUtils.configureStringProperty(configElem, ANNOTATION_NAME, true);
	}

	public String getAnnotationName() {
		return annotationName;
	}

	public abstract String renderAnnotation(AlignmentMember member);
	
}
