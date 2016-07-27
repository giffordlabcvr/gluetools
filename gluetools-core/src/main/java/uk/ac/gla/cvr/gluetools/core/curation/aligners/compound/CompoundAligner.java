package uk.ac.gla.cvr.gluetools.core.curation.aligners.compound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsComputeConstrained;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.compound.CompoundAligner.CompoundAlignerResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.compound.CompoundAlignerException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

/**
 * Takes a list of multiple aligners and runs them all. 
 * Then, result segments are combined. Segments from aligners earlier in the list are preferred.
 * 
 * @author joshsinger
 *
 */
@PluginClass(elemName="compoundAligner")
public class CompoundAligner extends Aligner<CompoundAlignerResult, CompoundAligner> implements SupportsComputeConstrained {

	private List<Aligner<?,?>> aligners = new ArrayList<Aligner<?,?>>();
	
	public CompoundAligner() {
		super();
		addModulePluginCmdClass(CompoundAlignCommand.class);
		addModulePluginCmdClass(CompoundFileAlignCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element alignersElem = PluginUtils.findConfigElement(configElem, "aligners");
		List<Element> alignerElems = GlueXmlUtils.findChildElements(alignersElem);
		for(Element alignerElem: alignerElems) {
			ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
			ModulePlugin<?> modulePlugin = pluginFactory.createFromElement(pluginConfigContext, alignerElem);
			if(!(modulePlugin instanceof Aligner)) {
				throw new CompoundAlignerException(Code.ELEMENT_IS_NOT_AN_ALIGNER, alignerElem.getNodeName());
			}
			aligners.add((Aligner<?,?>) modulePlugin);
		}
		
	}




	@Override
	public CompoundAlignerResult computeConstrained(CommandContext cmdContext,
			String refName, Map<String, DNASequence> queryIdToNucleotides) {
		List<AlignerResult> alignerResults = new ArrayList<AlignerResult>();
		
		for(Aligner<?,?> aligner: aligners) {
			alignerResults.add(aligner.computeConstrained(cmdContext, refName, queryIdToNucleotides));
		}
		
		final Map<String, List<QueryAlignedSegment>> queryIdToAlignedSegments = initFastaIdToAlignedSegments(queryIdToNucleotides.keySet());
		for(String queryId: queryIdToNucleotides.keySet()) {
			List<QueryAlignedSegment> finalAlignedSegs = new ArrayList<QueryAlignedSegment>();
			Comparator<IReferenceSegment> segmentComparator = new Comparator<IReferenceSegment>() {
				@Override
				public int compare(IReferenceSegment seg1, IReferenceSegment seg2) {
					return Integer.compare(seg1.getRefStart(), seg2.getRefStart());
				}};
			
			for(AlignerResult alignerResult: alignerResults) {
				List<QueryAlignedSegment> alignerSegsForQuery = alignerResult.getQueryIdToAlignedSegments().get(queryId);
				if(alignerSegsForQuery == null) {
					alignerSegsForQuery = new ArrayList<QueryAlignedSegment>();
				}
				finalAlignedSegs.addAll(ReferenceSegment.subtract(alignerSegsForQuery, finalAlignedSegs));
				Collections.sort(finalAlignedSegs, segmentComparator);
			}
			queryIdToAlignedSegments.put(queryId, finalAlignedSegs);
		}
		
		
		return new CompoundAlignerResult(queryIdToAlignedSegments);
		
	}

	
	
	
	
	
	
	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Aligner.AlignCommand> getComputeConstrainedCommandClass() {
		return CompoundAlignCommand.class;
	}
	
	public static class CompoundAlignerResult extends Aligner.AlignerResult {
		public CompoundAlignerResult(Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments) {
			super("compoundAlignerResult", fastaIdToAlignedSegments);
		}
	}

	@CommandClass(
			commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
			description = "Align sequence data to a reference", 
			docoptUsages = {}, 
			metaTags={  CmdMeta.inputIsComplex },
			furtherHelp = Aligner.ALIGN_COMMAND_FURTHER_HELP
			)
	public static class CompoundAlignCommand extends Aligner.AlignCommand<CompoundAligner.CompoundAlignerResult, CompoundAligner> {

		@Override
		protected CompoundAlignerResult execute(CommandContext cmdContext, CompoundAligner modulePlugin) {
			return modulePlugin.computeConstrained(cmdContext, getReferenceName(), getQueryIdToNucleotides());
		}
	}

	@CommandClass(
			commandWords = { Aligner.FILE_ALIGN_COMMAND_WORD }, 
			description = "Align sequence file to a reference", 
			docoptUsages = { Aligner.FILE_ALIGN_COMMAND_DOCOPT_USAGE },
			metaTags = {  CmdMeta.consoleOnly },
			furtherHelp = Aligner.FILE_ALIGN_COMMAND_FURTHER_HELP
			)
	public static class CompoundFileAlignCommand extends Aligner.FileAlignCommand<CompoundAligner.CompoundAlignerResult, CompoundAligner> {

		@Override
		protected CompoundAlignerResult execute(CommandContext cmdContext, CompoundAligner modulePlugin) {
			return modulePlugin.computeConstrained(cmdContext, getReferenceName(), getQueryIdToNucleotides((ConsoleCommandContext) cmdContext));
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerDataObjectNameLookup("referenceName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
				registerPathLookup("sequenceFileName", false);
			}
		}

	}

	@Override
	public boolean supportsComputeConstrained() {
		for(Aligner<?, ?> aligner : aligners) {
			if(!(aligner instanceof SupportsComputeConstrained)) {
				return false;
			}
			if(!((SupportsComputeConstrained) aligner).supportsComputeConstrained()) {
				return false;
			}
		}
		return true;
	}

}
