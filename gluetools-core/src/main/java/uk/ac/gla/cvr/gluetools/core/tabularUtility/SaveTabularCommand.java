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
package uk.ac.gla.cvr.gluetools.core.tabularUtility;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OutputStreamCommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@CommandClass(
		commandWords={"save-tabular"}, 
		description = "Save tabular data to a file", 
		docoptUsages = { "" },
		metaTags = {CmdMeta.consoleOnly, CmdMeta.inputIsComplex}
)
public class SaveTabularCommand extends BaseSaveTabularCommand<OkResult>{

	@Override
	protected OkResult saveData(CommandContext cmdContext, TabularUtility tabularUtility, String fileName,
			ElementTableResult elementTableResult) {
		((ConsoleCommandContext) cmdContext).saveCommandResult(outputStream -> {
			LineFeedStyle lineFeedStyle = LineFeedStyle.LF;
			if(System.getProperty("os.name").toLowerCase().contains("windows")) {
				lineFeedStyle = LineFeedStyle.CRLF;
			}
			OutputStreamCommandResultRenderingContext renderContext = 
					new OutputStreamCommandResultRenderingContext(outputStream, tabularUtility.getOutputFormat(), lineFeedStyle, true);
			renderContext.setNullRenderingString(tabularUtility.getNullRenderingString());
			renderContext.setTrimNullValues(tabularUtility.getTrimNullValues());
			return renderContext;

		}, fileName, elementTableResult);
		return new OkResult();
	}


	
}
