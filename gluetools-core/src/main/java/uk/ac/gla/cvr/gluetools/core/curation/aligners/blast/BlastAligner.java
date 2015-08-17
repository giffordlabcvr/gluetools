package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.BlastAlignerException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.programs.blast.refdb.BlastRefSeqDB;
import uk.ac.gla.cvr.gluetools.programs.blast.refdb.BlastRefSeqDB.SingleReferenceDB;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

@PluginClass(elemName="blastAligner")
public class BlastAligner extends Aligner<BlastAligner.BlastAlignerResult, BlastAligner> {

	public static String 
		BLASTN_EXECUTABLE_PROPERTY = "gluetools.core.programs.blast.blastn.executable"; 

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		addProvidedCmdClass(ShowAlignerCommand.class);
		addProvidedCmdClass(ConfigureAlignerCommand.class);
		addProvidedCmdClass(BlastAlignCommand.class);
	}
	
	
	@CommandClass(
			commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
			description = "Align sequence data to a reference using BLAST", 
			docoptUsages = { Aligner.ALIGN_COMMAND_DOCOPT_USAGE }, 
			docoptOptions = { Aligner.ALIGN_COMMAND_DOCOPT_OPTIONS }
	)
	public static class BlastAlignCommand extends Aligner.AlignCommand<BlastAligner.BlastAlignerResult, BlastAligner> {

		@Override
		protected BlastAlignerResult execute(CommandContext cmdContext, BlastAligner modulePlugin) {
			String refName = getReferenceName();
			SequenceFormat queryFormat = getQuerySequenceFormat();
			byte[] queryBytes = getQuerySequenceBytes();
			String queryFasta = 
					">query\n"+
					queryFormat.nucleotidesAsString(queryBytes)+
					"\n";
			SingleReferenceDB refDB = BlastRefSeqDB.getInstance().ensureSingleReferenceDB(cmdContext, refName);
			refDB.readLock().lock();
			List<AlignedSegment> alignedSegments = new ArrayList<AlignedSegment>();
			ProcessResult blastResult;
			try {
				String blastNexecutable = 
						cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(BLASTN_EXECUTABLE_PROPERTY);
				// run blast based on the ref DB.
				blastResult = ProcessUtils.runProcess(queryFasta.getBytes(), 
						blastNexecutable, 
						// supply reference DB
						"-db", refDB.getFilePath().getAbsolutePath(), 
						// outfmt 5 is XML
						"-outfmt", "14");
			} finally {
				refDB.readLock().unlock();
			}
			Document resultDoc;
			try {
				resultDoc = GlueXmlUtils.documentFromBytes(blastResult.getOutputBytes());
			} catch(Exception e) {
				throw new BlastAlignerException(Code.BLAST_OUTPUT_FORMAT_ERROR, e.getLocalizedMessage());
			}
			GlueXmlUtils.prettyPrint(resultDoc, System.out);
			return new BlastAlignerResult(alignedSegments);
		}
	
	}

	public static class BlastAlignerResult extends Aligner.AlignerResult {
		public BlastAlignerResult(List<AlignedSegment> alignedSegments) {
			super("blastAlignerResult", alignedSegments);
		}

	}
	
	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this aligner") 
	public static class ShowAlignerCommand extends ShowConfigCommand<BlastAligner> {}

	@SimpleConfigureCommandClass(
			propertyNames={}
	)
	public static class ConfigureAlignerCommand extends SimpleConfigureCommand<BlastAligner> {}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Aligner.AlignCommand> getAlignCommandClass() {
		return BlastAlignCommand.class;
	}

	
	
}
