package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.LinkedHashMap;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

// these FASTA files were generated when there was a bug in AllColumnsAlignment which missed off the last nucleotide.
@CommandClass(
		commandWords={"correct", "alignment"}, 
		description = "Correct FASTA files that miss off the last nucleotide", 
		docoptUsages = {"<filePath> <alignmentName>"}, 
		docoptOptions = {},
		metaTags={CmdMeta.updatesDatabase, CmdMeta.consoleOnly}
)
public class CorrectAlignmentCommand extends ProjectModeCommand<OkResult> {

	private String filePath;
	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		filePath = PluginUtils.configureStringProperty(configElem, "filePath", true);
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
	}

	
	@Override
	public OkResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		byte[] fastaBytes = consoleCommandContext.loadBytes(filePath);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		Map<String, DNASequence> idToSequence = FastaUtils.parseFasta(fastaBytes);
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Map<String, String> corrected = new LinkedHashMap<String, String>();
		idToSequence.forEach( (id, dnaSequence) -> {
			String[] bits = id.split("\\.");
			String sourceName = bits[1];
			String sequenceID = bits[2];
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID));
			AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, AlignmentMember.pkMap(alignmentName, sourceName, sequenceID));
			String sequenceNTs = sequence.getSequenceObject().getNucleotides(cmdContext);
			int length = sequenceNTs.length();
			int maxQueryNt = 0;
			for(AlignedSegment alignedSegment : almtMember.getAlignedSegments()) {
				maxQueryNt = Math.max(maxQueryNt, alignedSegment.getQueryEnd());
			}
			char lastSequenceNT = sequenceNTs.charAt(sequenceNTs.length() - 1);
			String sequenceAsString = dnaSequence.getSequenceAsString();
			if(maxQueryNt == length - 1) {
				int correctionLoc = sequenceAsString.length() - 1;
				while(sequenceAsString.charAt(correctionLoc-1) == '-') {
					correctionLoc --;
				}
				StringBuffer stringBuffer = new StringBuffer(sequenceAsString);
				stringBuffer.setCharAt(correctionLoc, lastSequenceNT);
				corrected.put(id, stringBuffer.toString());
			} else {
				// no correction needed
				corrected.put(id, sequenceAsString);
			}
		} );
		
		StringBuffer outputBuffer = new StringBuffer();
		corrected.forEach((id, nts) -> {
			outputBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(id, nts));
		});
		consoleCommandContext.saveBytes(filePath+".corrected", outputBuffer.toString().getBytes());
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("filePath", false);
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
		}
	}
	
}
