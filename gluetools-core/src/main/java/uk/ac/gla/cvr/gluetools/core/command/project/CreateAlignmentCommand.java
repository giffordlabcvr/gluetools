package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","alignment"}, 
	docoptUsages={"<alignmentName> <refSeqSourceName> <refSeqID>"},
	description="Create a new alignment, based on a reference sequence", 
	furtherHelp="An alignment is a proposed homology between certain segments of a reference sequence and certain segments"+
	" in zero or more member sequences. The reference sequence must be specified when the alignment is created."+
	" While a sequence is a reference sequence of an alignment, it may not be deleted.") 
public class CreateAlignmentCommand extends ProjectModeCommand {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String REF_SEQ_SOURCE_NAME = "refSeqSourceName";
	public static final String REF_SEQ_ID = "refSeqID";
	
	private String alignmentName;
	private String refSeqSourceName;
	private String refSeqID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		refSeqSourceName = PluginUtils.configureStringProperty(configElem, REF_SEQ_SOURCE_NAME, true);
		refSeqID = PluginUtils.configureStringProperty(configElem, REF_SEQ_ID, true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Sequence sequence = GlueDataObject.lookup(cmdContext.getObjectContext(), Sequence.class, 
				Sequence.pkMap(refSeqSourceName, refSeqID));
		Alignment alignment = GlueDataObject.create(objContext, Alignment.class, Alignment.pkMap(alignmentName), false, false);
		alignment.setRefSequence(sequence);
		alignment.setLive(true);
		return new CreateResult(Alignment.class, 1);
	}

}
