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
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsCommandModeTree {

	@PojoDocumentField
	public String modeDescription;

	@PojoDocumentField
	public String relativeModePath;

	@PojoDocumentField
	public String absoluteModePathID;
	
	@PojoDocumentListField(itemClass = WebdocsCommandModeTree.class)
	public List<WebdocsCommandModeTree> childCommandModes = new ArrayList<WebdocsCommandModeTree>();

	@SuppressWarnings("rawtypes")
	public static WebdocsCommandModeTree create(String relativeModePath, String absoluteModePathID, String modeDescription, CommandFactory commandFactory) {
		WebdocsCommandModeTree cmdModeTree = new WebdocsCommandModeTree();
		cmdModeTree.relativeModePath = relativeModePath;
		cmdModeTree.absoluteModePathID = absoluteModePathID;
		cmdModeTree.modeDescription = modeDescription;
		
		List<Class<? extends Command>> registeredCmdClasses = commandFactory.getRegisteredCommandClasses();
		registeredCmdClasses.forEach(cmdClass -> {
			EnterModeCommandClass enterModeAnno = cmdClass.getAnnotation(EnterModeCommandClass.class);
			if(enterModeAnno != null) {
				Class<? extends CommandFactory> childModeCmdFactoryClass = enterModeAnno.commandFactoryClass();
				CommandFactory childModeCmdFactory = CommandFactory.get(childModeCmdFactoryClass);
				String enterModeFirstCmdWord = CommandUsage.cmdWordsForCmdClass(cmdClass)[0];
				String[] modeIDs = CommandUsage.docoptUsagesForCmdClass(cmdClass)[0].split(" ");
				String childModeRelativeModePath = enterModeFirstCmdWord+"/"+
						String.join("/", modeIDs)+"/";
				String childAbsoluteModePathID = cmdModeTree.absoluteModePathID+"_"+enterModeFirstCmdWord;
				String childModeDescription = enterModeAnno.modeDescription();
				WebdocsCommandModeTree childCmdModeTree = create(childModeRelativeModePath, childAbsoluteModePathID, childModeDescription, childModeCmdFactory);
				cmdModeTree.childCommandModes.add(childCmdModeTree);
			}
		});
		return cmdModeTree;
	}
	
}
