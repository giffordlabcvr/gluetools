package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsCommandCategory {

	@PojoDocumentField
	public String description;
	
	@PojoDocumentListField(itemClass = WebdocsCommandDocumentation.class)
	public List<WebdocsCommandSummary> commandSummaries = new ArrayList<WebdocsCommandSummary>();

	public static WebdocsCommandCategory create(String description, List<WebdocsCommandSummary> commandSummaries) {
		WebdocsCommandCategory cmdCategory = new WebdocsCommandCategory();
		cmdCategory.description = description;
		cmdCategory.commandSummaries = commandSummaries;
		return cmdCategory;
	}
	
}
