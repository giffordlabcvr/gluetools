package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Optional;
import java.util.logging.Level;

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
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;


@CommandClass( 
		commandWords={"web-list", "member"},
		docoptUsages={"[-r] [-w <whereClause>] [-s <sortProperties>] [-f <outputFormat>] [-y <lineFeedStyle>] -o <fileName> [<fieldName> ...]"},
		docoptOptions={
				"-r, --recursive                                         Include descendent members",
				"-w <whereClause>, --whereClause <whereClause>           Qualify result set",
				"-f <outputFormat>, --outputFormat <outputFormat>        Result output format: CSV/TAB",
				"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>     Line feed style: LF/CRLF",
				"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties",
				"-o <fileName>, --fileName <fileName>                    Web file name"
			},
		description="List member sequences or field values",
		metaTags={CmdMeta.webApiOnly},
		furtherHelp=
		"The optional whereClause qualifies which alignment member are displayed.\n"+
		"If whereClause is not specified, all alignment members are displayed.\n"+
		"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  web-list member -w \"sequence.source.name = 'local'\" -o values.txt\n"+
		"  web-list member -w \"sequence.sequenceID like 'f%' and sequence.custom_field = 'value1'\" -o values.txt\n"+
		"  web-list member -o values.txt sequence.sequenceID sequence.custom_field"
	) 
public class AlignmentWebListMemberCommand extends AlignmentBaseListMemberCommand<CommandWebFileResult> {

	public static final String FILE_NAME = "fileName";
	public static final String LINE_FEED_STYLE = "lineFeedStyle";
	public static final String OUTPUT_FORMAT = "outputFormat";
	
	private String fileName;
	private LineFeedStyle lineFeedStyle;
	private ResultOutputFormat outputFormat;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
		this.outputFormat = Optional.ofNullable(PluginUtils.configureEnumProperty(ResultOutputFormat.class, configElem, OUTPUT_FORMAT,
				EnumSet.of(ResultOutputFormat.CSV, ResultOutputFormat.TAB), false)).orElse(ResultOutputFormat.TAB);
	}

	
	@Override
	public CommandWebFileResult execute(CommandContext cmdContext) {
		WebFilesManager webFilesManager = cmdContext.getGluetoolsEngine().getWebFilesManager();
		String subDirUuid = webFilesManager.createSubDir();
		webFilesManager.createWebFileResource(subDirUuid, fileName);
		
		AbstractListCTableDelegate delegate = super.getListCTableDelegate();
		
		int numMembers = AlignmentBaseListMemberCommand.countMembers(cmdContext, lookupAlignment(cmdContext), getRecursive(), delegate.getWhereClause());
		delegate.setWhereClause(Optional.of(getMatchExpression(lookupAlignment(cmdContext), getRecursive(), delegate.getWhereClause())));
		GlueLogger.getGlueLogger().log(Level.INFO, "processing "+numMembers+" alignment members");
		int offset = 0;
		int processed = 0;
		int batchSize = 500;
		while(offset < numMembers) {
			delegate.setFetchOffset(Optional.of(offset));
			delegate.setFetchLimit(Optional.of(batchSize));
			delegate.setPageSize(batchSize);
			ListResult batchResult = delegate.execute(cmdContext);
			try(OutputStream outputStream = webFilesManager.appendToWebFileResource(subDirUuid, fileName)) {
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, 65536);
				boolean renderHeaders = (offset == 0);
				batchResult.renderResult(new OutputStreamCommandResultRenderingContext(bufferedOutputStream, outputFormat, lineFeedStyle, renderHeaders));
			} catch(Exception e) {
				throw new CommandException(e, CommandException.Code.COMMAND_FAILED_ERROR, "Write to web file resource "+subDirUuid+"/"+fileName+" failed: "+e.getMessage());
			}
			processed += batchResult.getNumRows();
			GlueLogger.getGlueLogger().log(Level.INFO, "processed "+processed+" alignment members");
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		String webFileSizeString = webFilesManager.getSizeString(subDirUuid, fileName);
		return new CommandWebFileResult("webListMemberResult", subDirUuid, fileName, webFileSizeString);
	}
}