package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.GenbankXmlSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@PluginClass(elemName="fastaExporter")
public class FastaExporter extends AbstractFastaExporter<FastaExporter> {


	public FastaExporter() {
		super();
		addModulePluginCmdClass(ExportCommand.class);
		addModulePluginCmdClass(WebExportCommand.class);
		addModulePluginCmdClass(ExportMemberCommand.class);
		addModulePluginCmdClass(WebExportMemberCommand.class);
	}

	public byte[] doExport(CommandContext cmdContext, Expression whereClause, LineFeedStyle lineFeedStyle) {
		
		long startTime = System.currentTimeMillis();
		GenbankXmlSequenceObject.msInXPath = 0;
		GenbankXmlSequenceObject.msInDocParsing = 0;
		
		SelectQuery selectQuery = null;
		if(whereClause != null) {
			selectQuery = new SelectQuery(Sequence.class, whereClause);
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		int totalNumSeqs = GlueDataObject.count(cmdContext, selectQuery);
		int batchSize = 500;
		int offset = 0;
		selectQuery.setFetchLimit(batchSize);
		selectQuery.setPageSize(batchSize);
		StringBuffer stringBuffer = new StringBuffer();

		while(offset < totalNumSeqs) {
			selectQuery.setFetchOffset(offset);
			int lastBatchIndex = Math.min(offset+batchSize, totalNumSeqs);
			GlueLogger.getGlueLogger().info("Retrieving sequences "+(offset+1)+" to "+lastBatchIndex+" of "+totalNumSeqs);
			List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			GlueLogger.getGlueLogger().info("Processing sequences "+(offset+1)+" to "+lastBatchIndex+" of "+totalNumSeqs);
			sequences.forEach(seq -> {
				String fastaId = generateFastaId(seq);
				stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, seq.getSequenceObject().getNucleotides(cmdContext), lineFeedStyle));
			});
			offset += batchSize;
		}
		GlueLogger.getGlueLogger().finest("Time for doExport was "+(System.currentTimeMillis() - startTime)+"ms");
		GlueLogger.getGlueLogger().finest("Time in genbank sequence xpath was "+GenbankXmlSequenceObject.msInXPath+"ms");
		GlueLogger.getGlueLogger().finest("Time in genbank document parsing was "+GenbankXmlSequenceObject.msInDocParsing+"ms");

		
		return stringBuffer.toString().getBytes();
	}


	public byte[] doExportMembers(CommandContext cmdContext, String alignmentName, 
			Boolean recursive, Expression whereClause, LineFeedStyle lineFeedStyle) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Expression matchExp = AlignmentListMemberCommand.getMatchExpression(alignment, recursive, Optional.ofNullable(whereClause));
		SelectQuery selectQuery = new SelectQuery(AlignmentMember.class, matchExp);
		int totalNumAlmtMembers = GlueDataObject.count(cmdContext, selectQuery);
		int batchSize = 500;
		int offset = 0;
		selectQuery.setFetchLimit(batchSize);
		selectQuery.setPageSize(batchSize);
		StringBuffer stringBuffer = new StringBuffer();

		while(offset < totalNumAlmtMembers) {
			selectQuery.setFetchOffset(offset);
			int lastBatchIndex = Math.min(offset+batchSize, totalNumAlmtMembers);
			GlueLogger.getGlueLogger().info("Retrieving alignment members "+(offset+1)+" to "+lastBatchIndex+" of "+totalNumAlmtMembers);
			List<AlignmentMember> almtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
			GlueLogger.getGlueLogger().info("Processing alignment members "+(offset+1)+" to "+lastBatchIndex+" of "+totalNumAlmtMembers);
			almtMembers.forEach(almtMember -> {
				Sequence seq = almtMember.getSequence();
				String fastaId = generateFastaId(seq);
				stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, seq.getSequenceObject().getNucleotides(cmdContext), lineFeedStyle));
			});
			offset += batchSize;
		}
		return stringBuffer.toString().getBytes();
	}

	
	
	
}
