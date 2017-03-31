package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandBinaryResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;

@CommandClass( 
		commandWords={"web-export"}, 
		docoptUsages={"<alignmentName> [ -s <selectorName> | -r <relRefName> -f <featureName> [-l <lcStart> <lcEnd> | -n <ntStart> <ntEnd>] ] [-c] (-w <whereClause> | -a) [-e] [-d <orderStrategy>] [-i [-m <minColUsage>]]"},
		docoptOptions={
			"-s <selectorName>, --selectorName <selectorName>      Column selector module",
			"-r <relRefName>, --relRefName <relRefName>            Related reference",
			"-f <featureName>, --featureName <featureName>         Restrict to a given feature",
			"-l, --labelledCodon                                   Region between codon labels",
			"-n, --ntRegion                                        Specific nucleotide region",
			"-c, --recursive                                       Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>         Qualify exported members",
		    "-a, --allMembers                                      Export all members",
		    "-e, --excludeEmptyRows                                Exclude empty rows",
		    "-d <orderStrategy>, --orderStrategy <orderStrategy>   Specify row ordering strategy",
		    "-i, --includeAllColumns                               Include columns for all NTs",
		    "-m <minColUsage>, --minColUsage <minColUsage>         Minimum included column usage"},
		metaTags = { CmdMeta.webApiOnly, CmdMeta.producesBinary },
		description="Export nucleotide alignment (web API)", 
		furtherHelp="The --labeledCodon option may be used only for coding features.\n" 
				+ "If --ntRegion is used, the coordinates are relative to the named reference sequence.") 
public class FastaAlignmentWebExportCommand extends BaseFastaAlignmentExportCommand<CommandBinaryResult> {

	@Override
	public CommandBinaryResult execute(CommandContext cmdContext, FastaAlignmentExporter exporterPlugin) {
		String almtString = super.formAlmtString(cmdContext, exporterPlugin);
		return new CommandBinaryResult("fastaAlignmentWebExportResult", almtString.getBytes());
		
	}
	
}