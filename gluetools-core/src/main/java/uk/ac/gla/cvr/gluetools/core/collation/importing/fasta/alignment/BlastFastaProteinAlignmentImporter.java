package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner.BlastType;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.TemporarySingleSeqBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@PluginClass(elemName="blastProteinFastaAlignmentImporter")
public class BlastFastaProteinAlignmentImporter extends BaseFastaAlignmentImporter<BlastFastaProteinAlignmentImporter> {


	public static final String MIN_ROW_COVERAGE_PERCENT = "minRowCoveragePercent";

	private Double minRowCoveragePercent;

	
	private BlastRunner blastRunner = new BlastRunner();

	public BlastFastaProteinAlignmentImporter() {
		super();
		addModulePluginCmdClass(ImportCommand.class);
		addSimplePropertyName(MIN_ROW_COVERAGE_PERCENT);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		minRowCoveragePercent = Optional
				.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_ROW_COVERAGE_PERCENT, false))
				.orElse(95.0);

		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, "blastRunner");
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
	}

	public FastaProteinAlignmentImporterResult doImport(
			ConsoleCommandContext cmdContext, String fileName,
			String alignmentName, String sourceName) {
		Alignment alignment = initAlignment(cmdContext, alignmentName);
		byte[] fastaFileBytes = cmdContext.loadBytes(fileName);
		
		Map<String, ProteinSequence> sequenceMap = FastaUtils.parseFastaProtein(fastaFileBytes);
		
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		
		int alignmentRows = 0;
		for(Map.Entry<String, ProteinSequence> entry: sequenceMap.entrySet()) {
			String fastaID = entry.getKey();

			Sequence foundSequence = findSequence(cmdContext, fastaID, sourceName);
			if(foundSequence == null) {
				continue;
			}

			
			String alignmentAAsRow = entry.getValue().toString().toUpperCase();
			// The alignment row is in protein coordinates and has '-' gaps. TBLASTN will ignore these. 
			// The unconstrained alignment will have an implicit NT coordinate system.

			// We will remove the AA gaps and create a function which maps the gapless AA coordinate
			// to the gapped NT coordinate.
			
			Map<Integer, Integer> gaplessAAToGappedNT = new LinkedHashMap<Integer, Integer>();
			StringBuffer alignmentAAsRowGapless = new StringBuffer();
			int gaplessCoord = 1;
			for(int i = 1; i <= alignmentAAsRow.length(); i++) {
				char aa = SegmentUtils.base1Char(alignmentAAsRow, i);
				if(aa != '-') {
					alignmentAAsRowGapless.append(aa);
					gaplessAAToGappedNT.put(gaplessCoord, TranslationUtils.getNt(1, i));
					gaplessCoord++;
				}
			}
			Function<Integer, Integer> gaplessAAtoGappedNTMapper = new Function<Integer, Integer>(){
				@Override
				public Integer apply(Integer gaplessAACoord) {
					return gaplessAAToGappedNT.get(gaplessAACoord);
				}
			};
			
			String foundSequenceNTs = foundSequence.getSequenceObject().getNucleotides(cmdContext);
			List<BlastResult> blastResults = runTBlastN(cmdContext, "alignmentAAs", alignmentAAsRowGapless.toString(), "glueSequenceRef", foundSequenceNTs);
			
			Map<String, List<QueryAlignedSegment>> blastResultsToAlignedSegmentsMap = 
					BlastUtils.tBlastNResultsToAlignedSegmentsMap("glueSequenceRef", blastResults, null, gaplessAAtoGappedNTMapper);

			List<QueryAlignedSegment> queryAlignedSegs = blastResultsToAlignedSegmentsMap.getOrDefault("alignmentAAs", new ArrayList<QueryAlignedSegment>());

			queryAlignedSegs = queryAlignedSegs.stream().map(seg -> seg.invert()).collect(Collectors.toList());
			

			int alignedSegNTs = 0;
			for(QueryAlignedSegment queryAlignedSeg: queryAlignedSegs) {
				alignedSegNTs += queryAlignedSeg.getCurrentLength();
			}
			
			int alignmentRowNTs = alignmentAAsRowGapless.length() * 3;
			double aaCoveragePct = 0.0;
			if(alignmentRowNTs > 0) {
				aaCoveragePct = 100.0 * alignedSegNTs / (double) alignmentRowNTs;
			}
			
			if(aaCoveragePct < minRowCoveragePercent) {
				GlueLogger.getGlueLogger().warning("Skipping row with fasta ID "+fastaID+" row AA coverage percent "+aaCoveragePct+" < "+minRowCoveragePercent);
				continue;
			}

			AlignmentMember almtMember = ensureAlignmentMember(cmdContext, alignment, foundSequence);
			String memberSourceName = foundSequence.getSource().getName();
			String memberSequenceID = foundSequence.getSequenceID();

			for(QueryAlignedSegment queryAlignedSeg: queryAlignedSegs) {
				AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, 
						AlignedSegment.pkMap(alignmentName, memberSourceName, memberSequenceID, 
								queryAlignedSeg.getRefStart(), queryAlignedSeg.getRefEnd(), 
								queryAlignedSeg.getQueryStart(), queryAlignedSeg.getQueryEnd()), false);
				alignedSegment.setAlignmentMember(almtMember);
			}
			
			Map<String, Object> memberResultMap = new LinkedHashMap<String, Object>();
			memberResultMap.put("fastaID", fastaID);
			memberResultMap.put("sourceName", memberSourceName);
			memberResultMap.put("sequenceID", memberSequenceID);
			memberResultMap.put("numSegmentsAdded", queryAlignedSegs.size());
			memberResultMap.put("aaCoveragePct", aaCoveragePct);
			resultListOfMaps.add(memberResultMap);
			alignmentRows ++;
			if(alignmentRows % 25 == 0) {
				log("Imported "+alignmentRows+" alignment rows");
			}
			
		}
		log("Imported "+alignmentRows+" alignment rows");
		cmdContext.commit();
		
		
		return new FastaProteinAlignmentImporterResult(resultListOfMaps);
		
	}
	
	public static class FastaProteinAlignmentImporterResult extends TableResult {
		public FastaProteinAlignmentImporterResult(List<Map<String, Object>> rowData) {
			super("fastaProteinAlignmentImporterResult", Arrays.asList("fastaID", "sourceName", "sequenceID", "numSegmentsAdded", "aaCoveragePct"), rowData);
		}
	}

	
	
	private List<BlastResult> runTBlastN(CommandContext cmdContext, String queryAAFastaID, String queryAAs, String refFastaID, String referenceNTs) {
		String tempDbID = UUID.randomUUID().toString();
		String queryAAFastaRow = FastaUtils.seqIdCompoundsPairToFasta(queryAAFastaID, queryAAs, LineFeedStyle.LF);
		byte[] queryAAFastaBytes = queryAAFastaRow.getBytes();
		BlastDbManager blastDbManager = BlastDbManager.getInstance();
		List<BlastResult> blastResults = null;
		try {
			TemporarySingleSeqBlastDB tempBlastDB = 
					blastDbManager.createTempSingleSeqBlastDB(cmdContext, tempDbID, refFastaID, referenceNTs);
			blastResults = blastRunner.executeBlast(cmdContext, BlastType.TBLASTN, tempBlastDB, queryAAFastaBytes);
		} finally {
			blastDbManager.removeTempSingleSeqBlastDB(cmdContext, tempDbID);
		}
		return blastResults;
	}

	@CommandClass( 
			commandWords={"import"}, 
			docoptUsages={"<alignmentName> -f <fileName> [-s <sourceName>]"},
			docoptOptions={
			"-f <fileName>, --fileName <fileName>        FASTA file",
			"-s <sourceName>, --sourceName <sourceName>  Restrict alignment members to a given source"},
			description="Import an unconstrained alignment from a FASTA protein file", 
			metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
			furtherHelp="The file is loaded from a location relative to the current load/save directory. "+
			"An existing unconstrained alignment will be updated with new members, or a new unconstrained alignment will be created.") 
	public static class ImportCommand extends ModulePluginCommand<FastaProteinAlignmentImporterResult, BlastFastaProteinAlignmentImporter> implements ProvidedProjectModeCommand {

		private String fileName;
		private String alignmentName;
		private String sourceName;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
			alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
			sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		}
		
		@Override
		protected FastaProteinAlignmentImporterResult execute(CommandContext cmdContext, BlastFastaProteinAlignmentImporter importerPlugin) {
			return importerPlugin.doImport((ConsoleCommandContext) cmdContext, fileName, alignmentName, sourceName);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerVariableInstantiator("alignmentName", new VariableInstantiator() {
					@SuppressWarnings("rawtypes")
					@Override
					protected List<CompletionSuggestion> instantiate(
							ConsoleCommandContext cmdContext,
							Class<? extends Command> cmdClass, Map<String, Object> bindings,
							String prefix) {
						return GlueDataObject.query(cmdContext, Alignment.class, new SelectQuery(Alignment.class))
								.stream()
								.filter(almt -> !almt.isConstrained())
								.map(almt -> new CompletionSuggestion(almt.getName(), true))
								.collect(Collectors.toList());
					}
				});
				registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
				registerPathLookup("fileName", false);
			}
		}

	}
	

	
}
