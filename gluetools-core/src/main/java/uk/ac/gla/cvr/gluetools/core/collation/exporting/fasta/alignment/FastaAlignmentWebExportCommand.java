package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandWebFileResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager;

@CommandClass( 
		commandWords={"web-export"}, 
		docoptUsages={"<alignmentName> -r <relRefName> -f <featureName> [-l <lcStart> <lcEnd> | -n <ntStart> <ntEnd>] [-c] (-w <whereClause> | -a) [-e] -o <fileName>"},
		docoptOptions={
			"-r <relRefName>, --relRefName <relRefName>            Related reference",
			"-f <featureName>, --featureName <featureName>         Restrict to a given feature",
			"-l, --labelledCodon                                   Region between codon labels",
			"-n, --ntRegion                                        Specific nucleotide region",
			"-c, --recursive                                       Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>         Qualify exported members",
		    "-a, --allMembers                                      Export all members",
		    "-e, --excludeEmptyRows                                Exclude empty rows",
		    "-o <fileName>, --fileName <fileName>                  File name"
		},
		metaTags = { CmdMeta.webApiOnly },
		description="Export nucleotide alignment (web API)", 
		furtherHelp="The --labeledCodon option may be used only for coding features.\n" 
				+ "If --ntRegion is used, the coordinates are relative to the named reference sequence.") 
public class FastaAlignmentWebExportCommand extends BaseFastaAlignmentExportCommand<CommandWebFileResult> {

	public static final String FILE_NAME = "fileName";
	
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	@Override
	public CommandWebFileResult execute(CommandContext cmdContext, FastaAlignmentExporter exporterPlugin) {
		FastaAlignmentExportCommandDelegate delegate = getDelegate();
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(delegate.getAlignmentName()));
		if(!alignment.isConstrained()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Web export may only be applied to constrained alignments");
		};
		WebFilesManager webFilesManager = cmdContext.getGluetoolsEngine().getWebFilesManager();
		String subDirUuid = webFilesManager.createSubDir();
		webFilesManager.createWebFileResource(subDirUuid, fileName);
		int batchSize = 500;
		int numMembers = AlignmentListMemberCommand.countMembers(cmdContext, alignment, delegate.getRecursive(), delegate.getWhereClause());
		int processed = 0;
		int offset = 0;
		while(processed < numMembers) {
			List<AlignmentMember> almtMembers = 
					AlignmentListMemberCommand.listMembers(cmdContext, alignment, delegate.getRecursive(), delegate.getWhereClause(), 
							offset, batchSize, batchSize);
			try(OutputStream outputStream = webFilesManager.appendToWebFileResource(subDirUuid, fileName)) {
				PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(outputStream, 65536));
				FastaAlignmentExporter.webExportAlignment(cmdContext, alignment, almtMembers, 
						delegate.getExcludeEmptyRows(), exporterPlugin.getIdTemplate(), delegate.getLineFeedStyle(), 
						printWriter);
			} catch(Exception e) {
				throw new CommandException(e, Code.COMMAND_FAILED_ERROR, "Write to web file resource "+subDirUuid+"/"+fileName+" failed: "+e.getMessage());
			}
			processed += almtMembers.size();
			GlueLogger.getGlueLogger().finest("Processsed "+processed+" of "+numMembers);
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		String webFileSizeString = webFilesManager.getSizeString(subDirUuid, fileName);
		
		return new CommandWebFileResult("fastaAlignmentWebExportResult", subDirUuid, fileName, webFileSizeString);
	}
	
}