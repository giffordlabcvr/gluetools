package uk.ac.gla.cvr.gluetools.core.reporting.freemarkerDocTransformer;

import java.io.PrintWriter;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandWebFileResult;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager.WebFileType;

@CommandClass(
		commandWords={"transform-to-web-file"}, 
		description = "Transform a command document to a file available via the web files manager", 
		docoptUsages = {},
		docoptOptions = {},
		metaTags = {CmdMeta.webApiOnly, CmdMeta.inputIsComplex}	
)
public class TransformToWebFileCommand extends ModulePluginCommand<CommandWebFileResult, FreemarkerDocTransformer> {

	public final static String WEB_FILE_TYPE = "webFileType";
	public final static String COMMAND_DOCUMENT = "commandDocument";
	public static final String OUTPUT_FILE = "outputFile";

	private WebFileType webFileType;
	private String outputFile;
	private CommandDocument cmdDocument;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.webFileType = PluginUtils.configureEnumProperty(WebFileType.class, configElem, WEB_FILE_TYPE, true);
		this.cmdDocument = PluginUtils.configureCommandDocumentProperty(configElem, COMMAND_DOCUMENT, true);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
	}
	
	@Override
	protected CommandWebFileResult execute(CommandContext cmdContext, FreemarkerDocTransformer freemarkerTransformer) {
		WebFilesManager webFilesManager = cmdContext.getGluetoolsEngine().getWebFilesManager();
		String subDirUuid = webFilesManager.createSubDir(webFileType);
		webFilesManager.createWebFileResource(webFileType, subDirUuid, outputFile);

		try(PrintWriter printWriter = new PrintWriter(webFilesManager.appendToWebFileResource(webFileType, subDirUuid, outputFile))) {
			freemarkerTransformer.renderToWriter(cmdContext, cmdDocument, printWriter);
		} catch(Exception e) {
			throw new CommandException(e, Code.COMMAND_FAILED_ERROR, "Write to web file resource "+subDirUuid+"/"+outputFile+" failed: "+e.getMessage());
		}
		String webFileSizeString = webFilesManager.getSizeString(webFileType, subDirUuid, outputFile);
		
		return new CommandWebFileResult("freemarkerDocTransformerWebResult", webFileType, subDirUuid, outputFile, webFileSizeString);
	}

	
	
}
