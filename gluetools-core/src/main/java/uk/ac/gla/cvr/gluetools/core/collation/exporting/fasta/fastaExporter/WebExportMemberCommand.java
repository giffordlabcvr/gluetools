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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.fastaExporter;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.BaseExportMemberCommand;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.AbstractSequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.MemberQuerySequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandWebFileResult;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager.WebFileType;

@CommandClass( 
		commandWords={"web-export-member"}, 
		docoptUsages={"<alignmentName> [-c] [-w <whereClause>] [-y <lineFeedStyle>] -o <fileName>"},
		docoptOptions={
				"-c, --recursive                                      Include members of descendent alignments",
				"-w <whereClause>, --whereClause <whereClause>        Qualify exported members",
				"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>  LF or CRLF",
			    "-o <fileName>, --fileName <fileName>                 File name"
		},
		metaTags = { CmdMeta.webApiOnly },
		description="Export the sequences of alignment members to a FASTA file") 
public class WebExportMemberCommand extends BaseExportMemberCommand<CommandWebFileResult> implements ProvidedProjectModeCommand {

	public static final String FILE_NAME = "fileName";
	
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	public CommandWebFileResult execute(CommandContext cmdContext, FastaExporter fastaExporter) {
		WebFilesManager webFilesManager = cmdContext.getGluetoolsEngine().getWebFilesManager();
		String subDirUuid = webFilesManager.createSubDir(WebFileType.DOWNLOAD);
		webFilesManager.createWebFileResource(WebFileType.DOWNLOAD, subDirUuid, fileName);

		AbstractSequenceSupplier sequenceSupplier = 
				new MemberQuerySequenceSupplier(getAlignmentName(), getRecursive(), Optional.ofNullable(getWhereClause()));
		try(OutputStream outputStream = webFilesManager.appendToWebFileResource(WebFileType.DOWNLOAD, subDirUuid, fileName)) {
			PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(outputStream, 65536));
			super.export(cmdContext, sequenceSupplier, fastaExporter, printWriter);
		} catch(Exception e) {
			throw new CommandException(e, Code.COMMAND_FAILED_ERROR, "Write to web file resource "+subDirUuid+"/"+fileName+" failed: "+e.getMessage());
		}
		String webFileSizeString = webFilesManager.getSizeString(WebFileType.DOWNLOAD, subDirUuid, fileName);
		
		return new CommandWebFileResult("fastaWebExportMemberResult", WebFileType.DOWNLOAD, subDirUuid, fileName, webFileSizeString);
	}
	
}
