package uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class MemberAnnotationGenerator implements Plugin {

	public static final String ANNOTATION_NAME = "annotationName";
	
	private String annotationName;

	public MemberAnnotationGenerator() {
		super();
	}

	public MemberAnnotationGenerator(String annotationName) {
		super();
		this.annotationName = annotationName;
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.annotationName = PluginUtils.configureStringProperty(configElem, ANNOTATION_NAME, true);
	}

	public String getAnnotationName() {
		return annotationName;
	}


	public abstract String renderAnnotation(CommandContext cmdContext, AlignmentMember member);
	
	public static LinkedHashMap<String, String> generateAnnotations(CommandContext cmdContext, AlignmentMember almtMember,
			List<Object> annotationGeneratorsAndGroups) {
		LinkedHashMap<String, String> annotations = new LinkedHashMap<String, String>();
		for(Object obj: annotationGeneratorsAndGroups) {
			if(obj instanceof MemberAnnotationGenerator) {
				MemberAnnotationGenerator memberAnnotationGenerator = (MemberAnnotationGenerator) obj;
				annotations.put(memberAnnotationGenerator.getAnnotationName(), memberAnnotationGenerator.renderAnnotation(cmdContext, almtMember));
			} else {
				annotations.putAll(((AnnotationGeneratorGroup) obj).generateAnnotations(cmdContext, almtMember));
			}
		}
		return annotations;
	}

	public static List<Object> configureAnnotationGeneratorsAndGroups(PluginConfigContext pluginConfigContext, Element configElem) {
		MemberAnnotationGeneratorFactory annotationGeneratorFactory = PluginFactory.get(MemberAnnotationGeneratorFactory.creator);
		AnnotationGeneratorGroupFactory annotationGeneratorGroupFactory = PluginFactory.get(AnnotationGeneratorGroupFactory.creator);

		Set<String> generatorElemNames = annotationGeneratorFactory.getElementNames();
		Set<String> groupElemNames = annotationGeneratorGroupFactory.getElementNames();
		
		Set<String> bothElemNames = new LinkedHashSet<String>();
		bothElemNames.addAll(generatorElemNames);
		bothElemNames.addAll(groupElemNames);
		
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(bothElemNames);
		List<Element> annotationGeneratorElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		List<Object> annotationGeneratorsAndGroups = new ArrayList<Object>();
		
		for(Element elem: annotationGeneratorElems) {
			if(generatorElemNames.contains(elem.getNodeName())) {
				annotationGeneratorsAndGroups.add(annotationGeneratorFactory.createFromElement(pluginConfigContext, elem));
			} else {
				// group
				annotationGeneratorsAndGroups.add(annotationGeneratorGroupFactory.createFromElement(pluginConfigContext, elem));
			}
		}
		return annotationGeneratorsAndGroups;
	}
	
}
