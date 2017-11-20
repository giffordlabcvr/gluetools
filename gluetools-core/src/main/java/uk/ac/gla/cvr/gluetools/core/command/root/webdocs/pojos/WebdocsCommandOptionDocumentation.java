package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class WebdocsCommandOptionDocumentation {

	@PojoDocumentField
	public String shortForm;

	@PojoDocumentField
	public String longForm;

	@PojoDocumentField
	public String description;
	
	public static WebdocsCommandOptionDocumentation createFromString(String stringOptionLine) {
		int commaIndex = stringOptionLine.indexOf(",");
		int doubleSpaceIndex = stringOptionLine.indexOf("  ");
		WebdocsCommandOptionDocumentation optionDoc = new WebdocsCommandOptionDocumentation();
		optionDoc.shortForm = stringOptionLine.substring(0, commaIndex).trim();
		optionDoc.longForm = stringOptionLine.substring(commaIndex+1, doubleSpaceIndex).trim();
		optionDoc.description = stringOptionLine.substring(doubleSpaceIndex).trim();
		return optionDoc;
	}
}
