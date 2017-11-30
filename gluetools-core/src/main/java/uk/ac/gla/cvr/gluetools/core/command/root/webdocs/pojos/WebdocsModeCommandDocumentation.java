package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class WebdocsModeCommandDocumentation {

	@PojoDocumentField
	public String absoluteModePathID;

	@PojoDocumentField
	public String modeDescription;

	@PojoDocumentField
	public WebdocsCommandDocumentation commandDocumentation;
	
}
