/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsCommandDocumentation {

	@PojoDocumentListField(itemClass = String.class)
	public List<String> commandWords = new ArrayList<String>();

	// could be a more complex object based on the docopt FSM in future.
	@PojoDocumentListField(itemClass = String.class)
	public List<String> usagePatterns = new ArrayList<String>();

	// could be a more complex object
	@PojoDocumentListField(itemClass = WebdocsCommandOptionDocumentation.class)
	public List<WebdocsCommandOptionDocumentation> optionDocs = new ArrayList<WebdocsCommandOptionDocumentation>();

	@PojoDocumentField
	public String description;

	@PojoDocumentField
	public String docCategory;

	@PojoDocumentField
	public String furtherHelp;

	@SuppressWarnings("rawtypes")
	public static WebdocsCommandDocumentation createDocumentation(Class<? extends Command> cmdClass) {
		WebdocsCommandDocumentation cmdDoc = new WebdocsCommandDocumentation();
		CommandUsage cmdUsage = CommandUsage.commandUsageForCmdClass(cmdClass);
		cmdDoc.commandWords.addAll(Arrays.asList(cmdUsage.commandWords()));
		cmdDoc.usagePatterns.addAll(Arrays.asList(cmdUsage.docoptUsages()));
		List<String> optionStrings = Arrays.asList(cmdUsage.docoptOptions());
		optionStrings.forEach(optionString -> 
			cmdDoc.optionDocs.add(WebdocsCommandOptionDocumentation.createFromString(optionString)));
		cmdDoc.description = cmdUsage.description();
		cmdDoc.furtherHelp = cmdUsage.furtherHelp();
		return cmdDoc;
	}
}
