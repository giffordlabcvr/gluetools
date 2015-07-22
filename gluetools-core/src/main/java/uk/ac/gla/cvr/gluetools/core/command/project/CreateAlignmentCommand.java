package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","alignment"}, 
	docoptUsages={"<alignmentName> <refSeqName>"},
	description="Create a new alignment, based on a reference sequence", 
	furtherHelp="An alignment is a proposed homology between certain segments of a reference sequence and certain segments"+
	" in zero or more member sequences. The reference sequence must be specified when the alignment is created."+
	" While a reference sequence is referred to by an alignment, the reference sequence may not be deleted.") 
public class CreateAlignmentCommand extends ProjectModeCommand {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String REF_SEQ_NAME = "refSeqName";
	
	private String alignmentName;
	private String refSeqName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		refSeqName = PluginUtils.configureStringProperty(configElem, REF_SEQ_NAME, true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext.getObjectContext(), ReferenceSequence.class, 
				ReferenceSequence.pkMap(refSeqName));
		Alignment alignment = GlueDataObject.create(objContext, Alignment.class, Alignment.pkMap(alignmentName), false, false);
		alignment.setRefSequence(refSequence);
		alignment.setLive(true);
		return new CreateResult(Alignment.class, 1);
	}

}
