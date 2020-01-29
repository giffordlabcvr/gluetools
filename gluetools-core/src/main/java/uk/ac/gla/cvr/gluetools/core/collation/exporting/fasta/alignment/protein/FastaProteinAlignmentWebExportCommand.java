/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.AminoAcidFastaCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandWebFileResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager.WebFileType;
import uk.ac.gla.cvr.gluetools.utils.fasta.ProteinSequence;

@CommandClass( 
		commandWords={"web-export"}, 
		docoptUsages={"<alignmentName> (-s <selectorName> | -r <relRefName> -f <featureName> [-l <lcStart> <lcEnd>]) [-c] (-w <whereClause> | -a) [-e] (-o <fileName> | -p)"},
		docoptOptions={
			"-s <selectorName>, --selectorName <selectorName>     Columns selector module",
			"-r <relRefName>, --relRefName <relRefName>           Related reference",
			"-f <featureName>, --featureName <featureName>        Protein-coding feature",
			"-l, --labelledCodon                                  Region between codon labels",
			"-c, --recursive                                      Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>        Qualify exported members",
		    "-a, --allMembers                                     Export all members",
		    "-e, --excludeEmptyRows                               Exclude empty rows",
		    "-o <fileName>, --fileName <fileName>                 File name", 
			"-p, --preview                                        Preview output", 
		},
		metaTags = { CmdMeta.webApiOnly }, description = "Export protein alignment (web API)") 
public class FastaProteinAlignmentWebExportCommand extends BaseFastaProteinAlignmentExportCommand<CommandResult> {

	public static final String PREVIEW = "preview";
	public static final String FILE_NAME = "fileName";

	private Boolean preview;
	private String fileName;

	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		if(fileName == null && !preview || fileName != null && preview) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fileName> or <preview> must be specified, but not both");
		}
	}

	
	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaProteinAlignmentExporter almtExporter) {
		if(preview) {
			Map<String, ProteinSequence> proteinFastaMap = super.exportProteinAlignment(cmdContext, almtExporter);
			return new AminoAcidFastaCommandResult(proteinFastaMap);
		} else {
			WebFilesManager webFilesManager = cmdContext.getGluetoolsEngine().getWebFilesManager();
			String subDirUuid = webFilesManager.createSubDir(WebFileType.DOWNLOAD);
			webFilesManager.createWebFileResource(WebFileType.DOWNLOAD, subDirUuid, fileName);
			
			try(OutputStream outputStream = webFilesManager.appendToWebFileResource(WebFileType.DOWNLOAD, subDirUuid, fileName)) {
				PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(outputStream, 65536));
				super.exportProteinAlignment(cmdContext, almtExporter, printWriter);
			} catch(Exception e) {
				throw new CommandException(e, Code.COMMAND_FAILED_ERROR, "Write to web file resource "+subDirUuid+"/"+fileName+" failed: "+e.getMessage());
			}
			String webFileSizeString = webFilesManager.getSizeString(WebFileType.DOWNLOAD, subDirUuid, fileName);
			
			return new CommandWebFileResult("fastaProteinAlignmentWebExportResult", WebFileType.DOWNLOAD, subDirUuid, fileName, webFileSizeString);
		}
	}
	

}
