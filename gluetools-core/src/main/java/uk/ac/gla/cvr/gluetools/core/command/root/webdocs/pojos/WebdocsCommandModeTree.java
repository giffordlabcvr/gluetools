package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandFactory;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsCommandModeTree {

	@PojoDocumentField
	public String relativeModePath;

	@PojoDocumentField
	public String absoluteModePathID;
	
	@PojoDocumentListField(itemClass = WebdocsCommandModeTree.class)
	public List<WebdocsCommandModeTree> childCommandModes = new ArrayList<WebdocsCommandModeTree>();

	public static WebdocsCommandModeTree create(String relativeModePath, String absoluteModePathID, RootCommandFactory rootCommandFactory) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
