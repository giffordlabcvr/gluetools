package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

/*
 * A "detail" is a bit of markup on the sequence, indicating locations of interest.
 * Each detail has its own ID and a set of segments (at least 1), whose start / end points are expressed
 * in the sequence's coordinate space. Each segment within the detail may (optionally) also have its own id.
 * The visualise feature command will transform the coordinates of these segments to "u space".
 * 
 */
public class Detail implements Plugin {

	private String id;

	// where on the sequence are we talking about.
	private List<DetailSegment> detailSegments;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.id = PluginUtils.configureStringProperty(configElem, "id", true);
		List<Element> segmentElems = PluginUtils.findConfigElements(configElem, "segments", 1, null);
		this.detailSegments = PluginFactory.createPlugins(pluginConfigContext, DetailSegment.class, segmentElems);
	}

	public String getId() {
		return id;
	}

	public List<DetailSegment> getDetailSegments() {
		return detailSegments;
	}
	
	
	
}
