package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class WebdocsCommandModeSummary {


	@PojoDocumentField
	public String modePathID;

	@PojoDocumentField
	public String modePath;

	@PojoDocumentField
	public String description;
	
}
