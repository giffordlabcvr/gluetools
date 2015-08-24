package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
	commandWords={"remove", "segment"}, 
	docoptUsages={"<refStart> <refEnd>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Remove a segment of the reference sequence", 
	furtherHelp="") 
public class RemoveFeatureSegmentCommand extends FeatureLocModeCommand<DeleteResult> {

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
	public DeleteResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		DeleteResult result = GlueDataObject.delete(objContext, FeatureSegment.class, 
				FeatureSegment.pkMap(getRefSeqName(), getFeatureName(), refStart, refEnd));
		cmdContext.commit();
		return result;
	}

}
