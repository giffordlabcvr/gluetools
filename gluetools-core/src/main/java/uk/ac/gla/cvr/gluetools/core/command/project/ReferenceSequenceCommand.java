package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceSequenceMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceSequenceModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"reference"},
	docoptUsages={"<refSeqName>"},
	description="Enter command mode for a reference sequence")
@EnterModeCommandClass(
		commandFactoryClass = ReferenceSequenceModeCommandFactory.class)
public class ReferenceSequenceCommand extends ProjectModeCommand<OkResult>  {

	public static final String REF_SEQ_NAME = "refSeqName";
	private String refSeqName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSeqName = PluginUtils.configureStringProperty(configElem, REF_SEQ_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		
		ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName));
		cmdContext.pushCommandMode(new ReferenceSequenceMode(getProjectMode(cmdContext).getProject(), this, refSequence.getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends RefSeqNameCompleter {}
	

}
