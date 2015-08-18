package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompleterUtils {

	@SuppressWarnings("rawtypes")
	public static <E extends Enum<E>> List<String> enumCompletionSuggestions(Class<E> enumClass) {
			return Arrays.asList(enumClass.getEnumConstants()).stream().
					map(e -> e.name()).collect(Collectors.toList());
	}
	
	
}
