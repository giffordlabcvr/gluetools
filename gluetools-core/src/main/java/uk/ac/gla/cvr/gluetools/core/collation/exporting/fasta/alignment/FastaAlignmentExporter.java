package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.AbstractFastaExporter;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaAlignmentExporter")
public class FastaAlignmentExporter extends AbstractFastaExporter<FastaAlignmentExporter> {
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		addProvidedCmdClass(ExportCommand.class);
		addProvidedCmdClass(ShowExporterCommand.class);
		addProvidedCmdClass(ConfigureExporterCommand.class);
	}

	public OkResult doExport(ConsoleCommandContext cmdContext, String fileName, 
			String alignmentName, Boolean includeReference, Expression whereClause) {
		
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		SelectQuery selectQuery = null;
		Expression exp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignmentName);
		if(whereClause != null) {
			selectQuery = new SelectQuery(AlignmentMember.class, exp.andExp(whereClause));
		} else {
			selectQuery = new SelectQuery(AlignmentMember.class, exp);
		}
		List<AlignmentMember> almtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
		StringBuffer stringBuffer = new StringBuffer();
		ReferenceSequence refSequence = alignment.getRefSequence();
		int maxNtIndex = 0;
		if(refSequence != null) {
			maxNtIndex = refSequence.getSequence().getSequenceObject().getNucleotides(cmdContext).length();
		} else {
			for(AlignmentMember almtMember: almtMembers) {
				List<AlignedSegment> alignedSegments = almtMember.getAlignedSegments();
				if(!alignedSegments.isEmpty()) {
					AlignedSegment lastSegment = alignedSegments.get(alignedSegments.size()-1);
					maxNtIndex = Math.max(lastSegment.getRefEnd(), maxNtIndex);
				}
			}
		}
		String refSourceName = null;
		String refSeqID = null;
		if(includeReference && refSequence != null) {
			Sequence refSeqSeq = refSequence.getSequence();
			refSourceName = refSeqSeq.getSource().getName();
			refSeqID = refSeqSeq.getSequenceID();
			String refFastaID = generateFastaId(refSeqSeq);
			stringBuffer.append(FastaUtils.
					seqIdCompoundsPairToFasta(refFastaID, refSeqSeq.getSequenceObject().getNucleotides(cmdContext)));
		}
		for(AlignmentMember almtMember: almtMembers) {
			Sequence sequence = almtMember.getSequence();
			if(includeReference && 
				sequence.getSource().getName().equals(refSourceName) &&
				sequence.getSequenceID().equals(refSeqID)) {
				// don't include reference sequence twice.
				continue;
			}
			String fastaId = generateFastaId(sequence);
			int ntIndex = 1;
			StringBuffer nts = new StringBuffer();
			List<AlignedSegment> alignedSegments = almtMember.getAlignedSegments();
			AbstractSequenceObject seqObj = sequence.getSequenceObject();
			seqObj.getNucleotides(cmdContext);
			for(AlignedSegment seg: alignedSegments) {
				while(ntIndex < seg.getRefStart()) {
					nts.append("-");
					ntIndex++;
				}
				nts.append(seqObj.getNucleotides(cmdContext, seg.getMemberStart(), seg.getMemberEnd()));
				ntIndex = seg.getRefEnd()+1;
			}
			while(ntIndex <= maxNtIndex) {
				nts.append("-");
				ntIndex++;
			}
			stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, nts.toString()));
		}
		cmdContext.saveBytes(fileName, stringBuffer.toString().getBytes());
		return new OkResult();
	}

	
	
	@CommandClass( 
			commandWords={"export", "alignment"}, 
			docoptUsages={"<alignmentName> [-i] (-w <whereClause> | -a) -f <fileName>"},
			docoptOptions={
				"-i, --includeReference                         Include reference sequence",
				"-f <fileName>, --fileName <fileName>           FASTA file",
				"-w <whereClause>, --whereClause <whereClause>  Qualify exported members",
			    "-a, --allMembers                               Export all members"},
			metaTags = { CmdMeta.consoleOnly },
			description="Export alignment to a FASTA file", 
			furtherHelp="The file is saved to a location relative to the current load/save directory. "+
			"If --includeReference is specified, the reference sequence will be included and will be the first sequence in the output.") 
	public static class ExportCommand extends ModuleProvidedCommand<OkResult, FastaAlignmentExporter> implements ProvidedProjectModeCommand {

		private String fileName;
		private String alignmentName;
		private Expression whereClause;
		private Boolean allMembers;
		private Boolean includeReference;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
			alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
			whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
			allMembers = PluginUtils.configureBooleanProperty(configElem, "allMembers", true);
			includeReference = PluginUtils.configureBooleanProperty(configElem, "includeReference", true);
			if(whereClause == null && !allMembers) {
				usageError();
			}
			if(whereClause != null && allMembers) {
				usageError();
			}
		}

		private void usageError() {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allMembers> must be specified, but not both");
		}
		
		@Override
		protected OkResult execute(CommandContext cmdContext, FastaAlignmentExporter importerPlugin) {
			return importerPlugin.doExport((ConsoleCommandContext) cmdContext, fileName, alignmentName, includeReference, whereClause);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
				registerPathLookup("fileName", false);
			}
		}

	}
	
	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this exporter") 
	public static class ShowExporterCommand extends ShowConfigCommand<FastaAlignmentExporter> {}

	@SimpleConfigureCommandClass(
			propertyNames={"idTemplate"}
	)
	public static class ConfigureExporterCommand extends SimpleConfigureCommand<FastaAlignmentExporter> {
		
	}


	
	
}
