package uk.ac.gla.cvr.gluetools.core.command.project.alignment.feature;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
	commandWords={"remove", "segment"}, 
	docoptUsages={"<refStart> <refEnd>"},
	description="Remove a segment of the reference sequence", 
	furtherHelp="") 
public class RemoveFeatureSegmentCommand extends FeatureModeCommand {

	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	
	private int refStart;
	private int refEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, true);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		return GlueDataObject.delete(objContext, FeatureSegment.class, 
				FeatureSegment.pkMap(getAlignmentName(), getFeatureName(), refStart, refEnd));
	}

}
