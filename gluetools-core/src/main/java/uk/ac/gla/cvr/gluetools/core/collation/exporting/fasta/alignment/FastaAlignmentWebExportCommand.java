package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandBinaryResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"web-export"}, 
		docoptUsages={"<alignmentName> -r <relRefName> -f <featureName> [-l <lcStart> <lcEnd> | -n <ntStart> <ntEnd>] [-c] (-w <whereClause> | -a) -p <pageSize> -o <fetchOffset> [-e]"},
		docoptOptions={
			"-r <relRefName>, --relRefName <relRefName>            Related reference",
			"-f <featureName>, --featureName <featureName>         Restrict to a given feature",
			"-l, --labelledCodon                                   Region between codon labels",
			"-n, --ntRegion                                        Specific nucleotide region",
			"-c, --recursive                                       Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>         Qualify exported members",
		    "-a, --allMembers                                      Export all members",
			"-p <pageSize>, --pageSize <pageSize>                  Page size",
			"-o <fetchOffset>, --fetchOffset <fetchOffset>         Record number offset",
		    "-e, --excludeEmptyRows                                Exclude empty rows",
		    },
		metaTags = { CmdMeta.webApiOnly, CmdMeta.producesBinary },
		description="Export nucleotide alignment (web API)", 
		furtherHelp="The --labeledCodon option may be used only for coding features.\n" 
				+ "If --ntRegion is used, the coordinates are relative to the named reference sequence.") 
public class FastaAlignmentWebExportCommand extends BaseFastaAlignmentExportCommand<CommandBinaryResult> {

	public static final String PAGE_SIZE = "pageSize";
	public static final String FETCH_OFFSET = "fetchOffset";

	private Integer pageSize;
	private Integer fetchOffset;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.pageSize = PluginUtils.configureIntProperty(configElem, PAGE_SIZE, true);
		this.fetchOffset = PluginUtils.configureIntProperty(configElem, FETCH_OFFSET, true);
	}

	@Override
	public CommandBinaryResult execute(CommandContext cmdContext, FastaAlignmentExporter exporterPlugin) {
		FastaAlignmentExportCommandDelegate delegate = getDelegate();
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(delegate.getAlignmentName()));
		if(!alignment.isConstrained()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Web export may only be applied to constrained alignments");
		};
		List<AlignmentMember> almtMembers = 
				AlignmentListMemberCommand.listMembers(cmdContext, alignment, delegate.getRecursive(), delegate.getWhereClause(), 
						fetchOffset, pageSize, pageSize);
		String almtString = FastaAlignmentExporter.webExportAlignment(cmdContext, alignment, almtMembers, 
				delegate.getExcludeEmptyRows(), exporterPlugin.getIdTemplate(), delegate.getLineFeedStyle());
		return new CommandBinaryResult("fastaAlignmentWebExportResult", almtString.getBytes());
	}
	
}