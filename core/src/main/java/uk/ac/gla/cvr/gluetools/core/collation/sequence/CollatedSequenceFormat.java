package uk.ac.gla.cvr.gluetools.core.collation.sequence;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.gbflatfile.GenbankFlatFileUtils;

public enum CollatedSequenceFormat {

	GENBANK_FLAT_FILE {

		@Override
		public Document asXml(String sequenceText) {
			return GenbankFlatFileUtils.genbankFlatFileToXml(sequenceText);
		}
		
	};

	public abstract Document asXml(String sequenceText);
}
