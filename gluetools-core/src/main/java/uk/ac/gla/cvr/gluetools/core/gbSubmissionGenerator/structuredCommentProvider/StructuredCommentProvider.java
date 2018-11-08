package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.structuredCommentProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="structuredCommentProvider")
public class StructuredCommentProvider implements Plugin {

	private List<String> sequencingTechnologies = new ArrayList<String>();
	private List<String> assemblyMethods = new ArrayList<String>();

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.sequencingTechnologies = PluginUtils.configureStringsProperty(configElem, "sequencingTechnology");
		this.assemblyMethods = PluginUtils.configureStringsProperty(configElem, "assemblyMethod");
	}
	
	public Map<String, String> generateStructuredComments() {
		Map<String, String> structuredComments = new LinkedHashMap<String, String>();
		if(!assemblyMethods.isEmpty()) {
			structuredComments.put("Assembly Method", String.join("; ", assemblyMethods));
		}
		if(!sequencingTechnologies.isEmpty()) {
			structuredComments.put("Sequencing Technology", String.join("; ", sequencingTechnologies));
		}
		return structuredComments;
	}
	
}