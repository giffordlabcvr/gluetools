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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Optional;
import java.util.logging.Level;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandWebFileResult;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractListCTableCommand.AbstractListCTableDelegate;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OutputStreamCommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ResultOutputFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager.WebFileType;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;


@CommandClass( 
	commandWords={"web-list", "sequence"},
	docoptUsages={"[-w <whereClause>] [-s <sortProperties>] [-f <outputFormat>] [-y <lineFeedStyle>] -o <fileName> [<fieldName> ...]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>           Qualify result set",
		"-f <outputFormat>, --outputFormat <outputFormat>        Result output format: CSV/TAB",
		"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>     Line feed style: LF/CRLF",
		"-o <fileName>, --fileName <fileName>                    Web file name",
		"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties"},
	description="List sequences or sequence field values",
	metaTags={CmdMeta.webApiOnly},
	furtherHelp=
	"The optional whereClause qualifies which sequences are displayed.\n"+
	"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
	"Where fieldNames are specified, only these field values will be displayed.\n"+
	"Examples:\n"+
	"  web-list sequence -w \"source.name = 'local'\" -o metadata.txt\n"+
	"  web-list sequence -w \"sequenceID like 'f%' and custom_field = 'value1'\" -o metadata.txt\n"+
	"  web-list sequence -o metadata.txt sequenceID custom_field"
) 
public class WebListSequenceCommand extends ProjectModeCommand<CommandWebFileResult> {

	public static final String FILE_NAME = "fileName";
	public static final String LINE_FEED_STYLE = "lineFeedStyle";
	public static final String OUTPUT_FORMAT = "outputFormat";
	
	private String fileName;
	private LineFeedStyle lineFeedStyle;
	private ResultOutputFormat outputFormat;
	private AbstractListCTableDelegate listCTableDelegate = new AbstractListCTableDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.listCTableDelegate.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
		this.outputFormat = Optional.ofNullable(PluginUtils.configureEnumProperty(ResultOutputFormat.class, configElem, OUTPUT_FORMAT,
				EnumSet.of(ResultOutputFormat.CSV, ResultOutputFormat.TAB), false)).orElse(ResultOutputFormat.TAB);
	}
	
	public WebListSequenceCommand() {
		super();
		this.listCTableDelegate.setTableName(ConfigurableTable.sequence.name());
	}

	@Override
	public CommandWebFileResult execute(CommandContext cmdContext) {
		WebFilesManager webFilesManager = cmdContext.getGluetoolsEngine().getWebFilesManager();
		String subDirUuid = webFilesManager.createSubDir(WebFileType.DOWNLOAD);
		webFilesManager.createWebFileResource(WebFileType.DOWNLOAD, subDirUuid, fileName);
		
		AbstractListCTableDelegate delegate = this.listCTableDelegate;

		SelectQuery selectQuery;
		if(delegate.getWhereClause().isPresent()) {
			selectQuery = new SelectQuery(Sequence.class, delegate.getWhereClause().get());
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		int numSeqs = GlueDataObject.count(cmdContext, selectQuery);
		GlueLogger.getGlueLogger().log(Level.INFO, "processing "+numSeqs+" sequences");
		int offset = 0;
		int processed = 0;
		int batchSize = 500;
		while(offset < numSeqs) {
			delegate.setFetchOffset(Optional.of(offset));
			delegate.setFetchLimit(Optional.of(batchSize));
			delegate.setPageSize(batchSize);
			ListResult batchResult = delegate.execute(cmdContext);
			try(OutputStream outputStream = webFilesManager.appendToWebFileResource(WebFileType.DOWNLOAD, subDirUuid, fileName)) {
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, 65536);
				boolean renderHeaders = (offset == 0);
				batchResult.renderResult(new OutputStreamCommandResultRenderingContext(bufferedOutputStream, outputFormat, lineFeedStyle, renderHeaders));
			} catch(Exception e) {
				throw new CommandException(e, CommandException.Code.COMMAND_FAILED_ERROR, "Write to web file resource "+subDirUuid+"/"+fileName+" failed: "+e.getMessage());
			}
			processed += batchResult.getNumRows();
			GlueLogger.getGlueLogger().log(Level.INFO, "processed "+processed+" sequences");
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		String webFileSizeString = webFilesManager.getSizeString(WebFileType.DOWNLOAD, subDirUuid, fileName);
		return new CommandWebFileResult("webListSequenceResult", WebFileType.DOWNLOAD, subDirUuid, fileName, webFileSizeString);
	}


}
