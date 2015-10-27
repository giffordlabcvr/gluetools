package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","alignment"}, 
	docoptUsages={"<alignmentName> [-r <refSeqName>] "},
	docoptOptions={"-r <refSeqName>, --refSeqName <refSeqName>  Constraining reference sequence"},
	description="Create a new alignment, optionally constrained to a reference sequence", 
	metaTags={CmdMeta.updatesDatabase},
	furtherHelp="An alignment is container for a proposed homology between segments of certain sequences. "+
	"Alignments may be defined with a reference sequence, in which case they are constrained alignments. "+
	"Constrained alignments propose pairwise homologies between the reference and zero or more member sequences. "+
	"The reference coordinates of constrained alignment members refer to positions on the reference sequence. "+
	"Where used, the reference sequence must be specified when the alignment is created. "+
	"As long as a reference sequence constrains an alignment, the reference sequence may not be deleted."+
	"Unconstrained alignments do not have a reference sequence defined. Unconstrained alignments may propose "+
	"homologies between any of their members. The reference coordinates of unconstrained alignments do not refer to locations "+
	"on any sequence: these are used as a neutral coordinate system which can flexibly accommodate any homology."
	) 
public class CreateAlignmentCommand extends ProjectModeCommand<CreateResult> {

	public static final String REF_SEQ_NAME = "refSeqName";
	public static final String ALIGNMENT_NAME = "alignmentName";
	
	private String refSeqName;
	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSeqName = PluginUtils.configureStringProperty(configElem, REF_SEQ_NAME, false);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		ReferenceSequence refSequence = null;
		if(refSeqName != null) {
			refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
					ReferenceSequence.pkMap(refSeqName));
		}
		Alignment alignment = GlueDataObject.create(cmdContext, Alignment.class, Alignment.pkMap(alignmentName), false);
		if(refSequence != null) {
			alignment.setRefSequence(refSequence);
		}
		cmdContext.commit();
		return new CreateResult(Alignment.class, 1);
	}

	@CompleterClass
	public static class Completer extends RefSeqNameCompleter {}
}
