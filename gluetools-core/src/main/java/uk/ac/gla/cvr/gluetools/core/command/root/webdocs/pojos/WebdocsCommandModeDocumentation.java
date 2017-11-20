package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsCommandModeDocumentation {

	@PojoDocumentField
	public String relativeModePath;

	@PojoDocumentField
	public String absoluteModePathID;
	
	@PojoDocumentListField(itemClass = WebdocsCommandCategory.class)
	public List<WebdocsCommandCategory> commandCategories = new ArrayList<WebdocsCommandCategory>();
	
	
	
}
