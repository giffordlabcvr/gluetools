package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsModuleTypeCategory {

	@PojoDocumentField
	public String description;
	
	@PojoDocumentListField(itemClass = WebdocsModuleTypeSummary.class)
	public List<WebdocsModuleTypeSummary> moduleTypeSummaries = new ArrayList<WebdocsModuleTypeSummary>();

	public static WebdocsModuleTypeCategory create(String description, List<WebdocsModuleTypeSummary> moduleTypeSummaries) {
		WebdocsModuleTypeCategory moduleTypeCategory = new WebdocsModuleTypeCategory();
		moduleTypeCategory.description = description;
		moduleTypeCategory.moduleTypeSummaries = moduleTypeSummaries;
		return moduleTypeCategory;
	}
	
}
