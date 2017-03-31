package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandBinaryResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass( 
		commandWords={"web-export"}, 
		docoptUsages={"<alignmentName> -r <relRefName> -f <featureName> [-l <lcStart> <lcEnd>] [-c] (-w <whereClause> | -a) [-e] [-d <orderStrategy>]"},
		docoptOptions={
			"-r <relRefName>, --relRefName <relRefName>           Related reference",
			"-f <featureName>, --featureName <featureName>        Protein-coding feature",
			"-l, --labelledCodon                                  Region between codon labels",
			"-c, --recursive                                      Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>        Qualify exported members",
		    "-a, --allMembers                                     Export all members",
		    "-e, --excludeEmptyRows                               Exclude empty rows",
			"-d <orderStrategy>, --orderStrategy <orderStrategy>  Specify row ordering strategy", 
		},
		metaTags = { CmdMeta.webApiOnly, CmdMeta.producesBinary }, description = "Export protein alignment (web API)") 
public class FastaProteinAlignmentWebExportCommand extends BaseFastaProteinAlignmentExportCommand<CommandBinaryResult> {

	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}
	
	@Override
	protected CommandBinaryResult execute(CommandContext cmdContext, FastaProteinAlignmentExporter exporterPlugin) {
		String almtString = super.formAlmtString(cmdContext, exporterPlugin);
		return new CommandBinaryResult("fastaProteinAlignmentWebExportResult", almtString.getBytes());
	}
	

}