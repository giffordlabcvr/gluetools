package uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class AnnotationGeneratorGroup implements Plugin {

	public static final String ANNOTATION_NAME_PREFIX = "annotationNamePrefix";
	
	private String annotationNamePrefix;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.annotationNamePrefix = PluginUtils.configureStringProperty(configElem, ANNOTATION_NAME_PREFIX, true);
	}

	public abstract Map<String, String> generateAnnotations(CommandContext cmdContext, AlignmentMember almtMember);

	protected String getAnnotationNamePrefix() {
		return annotationNamePrefix;
	}
	
	public abstract List<String> getAnnotationNames(CommandContext cmdContext);
	
	
}
