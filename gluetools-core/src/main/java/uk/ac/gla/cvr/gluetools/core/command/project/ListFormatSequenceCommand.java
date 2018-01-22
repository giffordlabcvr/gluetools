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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;


@CommandClass( 
		commandWords={"list", "format", "sequence"},
		docoptUsages={""},
		description="List the sequence file formats which may be used") 
public class ListFormatSequenceCommand extends ProjectModeCommand<ListFormatSequenceCommand.ListFormatSequencesResult> {

	public static final String NAME = "name";
	public static final String DISPLAY_NAME = "displayName";
	public static final String FORMAT_URL = "formatURL";
	public static final String STD_FILE_EXTENSION = "standardFileExtension";

	@Override
	public ListFormatSequencesResult execute(CommandContext cmdContext) {
		return new ListFormatSequencesResult(cmdContext, Arrays.asList(SequenceFormat.values()));
	}

	
	public static class ListFormatSequencesResult extends TableResult {


		public ListFormatSequencesResult(CommandContext cmdContext, List<SequenceFormat> formats) {
			super("listFormatSequencesResult", Arrays.asList(NAME, DISPLAY_NAME, FORMAT_URL, STD_FILE_EXTENSION), 
					formats.stream()
						.map(seqFmt -> {
							Map<String, Object> map = new LinkedHashMap<String, Object>();
							map.put(NAME, seqFmt.name());
							map.put(DISPLAY_NAME, seqFmt.getDisplayName());
							map.put(FORMAT_URL, seqFmt.getFormatURL());
							map.put(STD_FILE_EXTENSION, seqFmt.getGeneratedFileExtension(cmdContext));
							return map;
						})
						.collect(Collectors.toList()));
		}
		

	}


}
