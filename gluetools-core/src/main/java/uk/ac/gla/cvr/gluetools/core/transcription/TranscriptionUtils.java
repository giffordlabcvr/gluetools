package uk.ac.gla.cvr.gluetools.core.transcription;

public class TranscriptionUtils {

	public static TranscriptionFormat transcriptionFormatFromString(String formatString) {
		try {
			return TranscriptionFormat.valueOf(formatString);
		} catch(IllegalArgumentException iae) {
			throw new TranscriptionException(TranscriptionException.Code.UNKNOWN_TRANSCRIPTION_TYPE, formatString);
		}

	}
	 
}
