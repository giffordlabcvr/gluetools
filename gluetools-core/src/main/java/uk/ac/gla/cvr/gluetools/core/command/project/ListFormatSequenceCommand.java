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

	@Override
	public ListFormatSequencesResult execute(CommandContext cmdContext) {
		return new ListFormatSequencesResult(Arrays.asList(SequenceFormat.values()));
	}

	
	public static class ListFormatSequencesResult extends TableResult {


		public ListFormatSequencesResult(List<SequenceFormat> formats) {
			super("listFormatSequencesResult", Arrays.asList(NAME, DISPLAY_NAME, FORMAT_URL), 
					formats.stream()
						.map(seqFmt -> {
							Map<String, Object> map = new LinkedHashMap<String, Object>();
							map.put(NAME, seqFmt.name());
							map.put(DISPLAY_NAME, seqFmt.getDisplayName());
							map.put(FORMAT_URL, seqFmt.getFormatURL());
							return map;
						})
						.collect(Collectors.toList()));
		}
		

	}


}