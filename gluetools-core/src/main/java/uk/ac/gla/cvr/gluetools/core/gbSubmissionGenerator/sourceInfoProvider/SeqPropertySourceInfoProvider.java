package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.sourceInfoProvider;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.RenderContext;
import uk.ac.gla.cvr.gluetools.utils.RenderUtils;

@PluginClass(elemName="seqPropertySourceInfoProvider")
public class SeqPropertySourceInfoProvider extends SourceInfoProvider {

	private String propertyPath;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.propertyPath = PluginUtils.configureStringProperty(configElem, "propertyPath", true);
	}

	@Override
	public String provideSourceInfo(Sequence sequence) {
		return RenderUtils.render(sequence.readNestedProperty(propertyPath), new RenderContext() {
			@Override
			public String renderNull() {
				return "";
			}
		});
	}
	
}
