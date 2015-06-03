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

	public static List<String> divideConcatenatedGBFiles(String concatenated) {
		List<String> individualGBFiles = new ArrayList<String>();
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
	
	
	public static Map<String, DNASequence> inputStreamToBioJavaDNASequences(InputStream inputStream) 
			throws IOException, GenbankFlatFileException {
		GenbankReader<DNASequence, NucleotideCompound> dnaReader = new GenbankReader<DNASequence, NucleotideCompound>(
				inputStream, 
		        new GenericGenbankHeaderParser<DNASequence,NucleotideCompound>(),
		        new DNASequenceCreator(AmbiguityDNACompoundSet.getDNACompoundSet())
		);
		try {
			return dnaReader.process();
		} catch (CompoundNotFoundException e) {
			throw new GenbankFlatFileException(e, Code.COMPOUND_NOT_FOUND);
		}
	}
	
	public static Document genbankFlatFileToXml(String genbankFlatFileString) throws GenbankFlatFileException {
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
		Element genbankFileElem = (Element) document.appendChild(document.createElement("genbankFile"));
		genbankFileElem.setAttribute("sequenceID", sequences.keySet().iterator().next());
		DNASequence dnaSequence = sequences.values().iterator().next();
		@SuppressWarnings("rawtypes")		
		List<FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound>> features = 
			dnaSequence.getFeatures();
		features.forEach(feature -> {
			Element featureElem = (Element) genbankFileElem.appendChild(document.createElement("feature"));
			Element shortDescElem = (Element) featureElem.appendChild(document.createElement("shortDesc"));
			shortDescElem.appendChild(document.createTextNode(feature.getShortDescription()));
			feature.getQualifiers().forEach( (qName, qualifier) -> {
				String value = qualifier.getValue();
				Element qualifierElem = (Element) featureElem.appendChild(document.createElement("qualifier"));
				qualifierElem.setAttribute("name", qName);
				qualifierElem.appendChild(document.createTextNode(value));
			} );
		});
		return document;
	}
}
