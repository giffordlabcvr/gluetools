package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"list", "clade-category"}, 
		description = "List the set of clade categories", 
		docoptUsages = { "" },
		docoptOptions = { },
		metaTags = {}	
)
public class ListCladeCategoryCommand extends ModuleDocumentCommand<ListCladeCategoryCommand.Result> {

	@Override
	protected Result processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		List<Element> cladeCategoryElems = GlueXmlUtils.findChildElements(modulePluginDoc.getDocumentElement(), "cladeCategory");
		return new Result(cladeCategoryElems);
	}

	public static class Result extends BaseTableResult<Element> {
		public Result(List<Element> cladeCategoryElems) {
			super("listCladeCategoryResult", cladeCategoryElems, 
					column("name", element -> GlueXmlUtils.getXPathString(element, "name/text()")),
					column("displayName", element -> GlueXmlUtils.getXPathString(element, "displayName/text()")),
					column("whereClause", element -> GlueXmlUtils.getXPathString(element, "whereClause/text()")),
					column("distanceScalingExponent", element -> GlueXmlUtils.getXPathString(element, "distanceScalingExponent/text()")),
					column("distanceCutoff", element -> GlueXmlUtils.getXPathString(element, "distanceCutoff/text()")),
					column("finalCladeCutoff", element -> GlueXmlUtils.getXPathString(element, "finalCladeCutoff/text()")));
		}
		
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}
	
}
