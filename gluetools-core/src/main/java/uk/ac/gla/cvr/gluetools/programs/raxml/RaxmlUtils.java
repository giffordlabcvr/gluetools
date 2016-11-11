package uk.ac.gla.cvr.gluetools.programs.raxml;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public class RaxmlUtils {

	public static String 
		RAXMLHPC_EXECUTABLE_PROPERTY = "gluetools.core.programs.raxml.raxmlhpc.executable"; 

	public static String 
		RAXML_TEMP_DIR_PROPERTY = "gluetools.core.programs.raxml.temp.dir";

	public static String 
		RAXMLHPC_NUMBER_CPUS = "gluetools.core.programs.raxml.raxmlhpc.cpus";

	
	public static boolean validRaxmlName(String string) {
		if(string.length() < 1 || string.length() > 256) {
			return false;
		}
		if(!string.matches("[^ \t\n\r\f:\\(\\)\\[\\]]+")) {
			return false;
		}
		return true;
	}

	/**
	 * Given an input alignment using keys of type D
	 * @return an alignment using RAxML-compatible string keys, populating two maps to map the keys between each other.
	 */
	public static <D> Map<String, DNASequence> remapAlignment(
			Map<D, DNASequence> keyToAlignmentRow,
			Map<String, D> rowNameToKey, Map<D, String> keyToRowName, String prefix) {
		int rowNameIndex = 0;
		Map<String, DNASequence> almtFastaContent = new LinkedHashMap<String, DNASequence>();
		for(Map.Entry<D, DNASequence> entry: keyToAlignmentRow.entrySet()) {
			String raxmlRowString = prefix+rowNameIndex;
			D key = entry.getKey();
			GlueLogger.log(Level.FINEST, "Mapped sequence "+key+" as "+raxmlRowString);
			rowNameToKey.put(raxmlRowString, key);
			keyToRowName.put(key, raxmlRowString);
			almtFastaContent.put(raxmlRowString, entry.getValue());
			rowNameIndex++;
		}
		return almtFastaContent;
	}

	
}
