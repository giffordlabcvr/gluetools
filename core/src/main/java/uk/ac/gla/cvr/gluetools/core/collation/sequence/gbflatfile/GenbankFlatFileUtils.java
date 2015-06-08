package uk.ac.gla.cvr.gluetools.core.collation.sequence.gbflatfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.FeatureInterface;
import org.biojava.nbio.core.sequence.io.DNASequenceCreator;
import org.biojava.nbio.core.sequence.io.GenbankReader;
import org.biojava.nbio.core.sequence.io.GenericGenbankHeaderParser;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.gbflatfile.GenbankFlatFileException.Code;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class GenbankFlatFileUtils {

	public static List<Object> divideConcatenatedGBFiles(String concatenated) {
		List<Object> individualGBFiles = new ArrayList<Object>();
		int startIndex = 0;
		String delimiter = "\n//";
		int matchLoc, endIndex;
		do {
			matchLoc = concatenated.indexOf(delimiter, startIndex);
			endIndex = matchLoc + delimiter.length();
			String extracted = "";
			if(startIndex < concatenated.length() && matchLoc < 0) {
				extracted = concatenated.substring(startIndex);
			} else if(matchLoc > 0) {
				extracted = concatenated.substring(startIndex, endIndex);
			}
			if(extracted.trim().length() > 0) {
				individualGBFiles.add(extracted);
			}
			startIndex = endIndex+1;
		} while(matchLoc > 0);
		return individualGBFiles;
	}
	
	
	public static Map<String, DNASequence> inputStreamToBioJavaDNASequences(InputStream inputStream) throws IOException 
			 {
		GenbankReader<DNASequence, NucleotideCompound> dnaReader = new GenbankReader<DNASequence, NucleotideCompound>(
				inputStream, 
		        new GenericGenbankHeaderParser<DNASequence,NucleotideCompound>(),
		        new DNASequenceCreator(AmbiguityDNACompoundSet.getDNACompoundSet())
		);
		try {
			return dnaReader.process();
		} catch (CompoundNotFoundException e) {
			throw new GenbankFlatFileException(e, Code.COMPOUND_NOT_FOUND, e.getLocalizedMessage());
		}
	}
	
	public static Document genbankFlatFileToXml(String genbankFlatFileString) {
		Map<String, DNASequence> sequences;
		try {
			sequences = GenbankFlatFileUtils.inputStreamToBioJavaDNASequences(new ByteArrayInputStream(genbankFlatFileString.getBytes()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(sequences.size() == 0) {
			throw new GenbankFlatFileException(Code.GENBANK_PARSING_FAILED);
		} else if(sequences.size() > 1) {
			throw new GenbankFlatFileException(Code.MULTIPLE_GENBANK_FILES_PARSED);
		}
		Document document = XmlUtils.newDocument();
		Element gbSeqElem = (Element) document.appendChild(document.createElement("GBSeq"));
		Element primaryAccElem = (Element) gbSeqElem.appendChild(document.createElement("GBSeq_primary-accession"));
		primaryAccElem.appendChild(document.createTextNode(sequences.keySet().iterator().next()));
	
		DNASequence dnaSequence = sequences.values().iterator().next();
		@SuppressWarnings("rawtypes")		
		List<FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound>> features = 
			dnaSequence.getFeatures();
		Element gbSeqFeatTableElem = (Element) gbSeqElem.appendChild(document.createElement("GBSeq_feature-table"));

		features.forEach(feature -> {
			Element gbFeatureElem = (Element) gbSeqFeatTableElem.appendChild(document.createElement("GBFeature"));
			Element gbFeatureKeyElem = (Element) gbFeatureElem.appendChild(document.createElement("GBFeature_key"));
			gbFeatureKeyElem.appendChild(document.createTextNode(feature.getShortDescription()));
			Element gbFeatureQualsElem = (Element) gbFeatureElem.appendChild(document.createElement("GBFeature_quals"));
			feature.getQualifiers().forEach( (qName, qualifier) -> {
				Element gbQualiferElem = (Element) gbFeatureQualsElem.appendChild(document.createElement("GBQualifier"));
				String value = qualifier.getValue();
				Element gbQualifierNameElem = (Element) gbQualiferElem.appendChild(document.createElement("GBQualifier_name"));
				gbQualifierNameElem.appendChild(document.createTextNode(qName));
				Element gbQualifierValueElem = (Element) gbQualiferElem.appendChild(document.createElement("GBQualifier_value"));
				gbQualifierValueElem.appendChild(document.createTextNode(value));
			} );
		});
		return document;
	}
}
