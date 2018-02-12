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
package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.AccessionID;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.io.template.SequenceHeaderParserInterface;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.importing.SequenceImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaFieldParser.Result;
import uk.ac.gla.cvr.gluetools.core.collation.populating.ValueExtractor;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.SequencePopulator.PropertyUpdate;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaImporter",
	description="Imports nucleotide data from a FASTA file, creating a set of Sequence objects")

public class FastaImporter extends SequenceImporter<FastaImporter> implements ValueExtractor, PropertyPopulator {

	private static final String SKIP_EXISTING_SEQUENCES = "skipExistingSequences";
	private static final String SOURCE_NAME = "sourceName";
	
	private Pattern nullRegex = null;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters = null;
	private String sourceName;
	private List<FastaFieldParser> fieldParsers;
	private boolean skipExistingSequences = false;

	public FastaImporter() {
		super();
		registerModulePluginCmdClass(FastaImporterImportCommand.class);
		addSimplePropertyName(SKIP_EXISTING_SEQUENCES);
		addSimplePropertyName(SOURCE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = Optional.ofNullable(PluginUtils.
				configureStringProperty(configElem, SOURCE_NAME, false)).orElse("local");
		skipExistingSequences = Optional.ofNullable(PluginUtils.
				configureBooleanProperty(configElem, SKIP_EXISTING_SEQUENCES, false)).orElse(false);
		List<Element> idParserElems = PluginUtils.findConfigElements(configElem, "idParser", 0, 1);
		if(!idParserElems.isEmpty()) {
			Element idParserElem  = idParserElems.get(0);
			nullRegex = Optional.ofNullable(
					PluginUtils.configureRegexPatternProperty(idParserElem, "nullRegex", false)).
					orElse(Pattern.compile(ValueExtractor.DEFAULT_NULL_REGEX));
			valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
					PluginUtils.findConfigElements(idParserElem, "valueConverter"));
			mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, idParserElem);
		}
		List<Element> fieldParserElems = PluginUtils.findConfigElements(configElem, "fieldParser");
		fieldParsers = PluginFactory.createPlugins(pluginConfigContext, FastaFieldParser.class, fieldParserElems);
	}

	public CreateResult doImport(ConsoleCommandContext cmdContext, String fileName) {
		byte[] fastaBytes = cmdContext.loadBytes(fileName);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		HeaderParser headerParser = new HeaderParser();
		Map<String, DNASequence> idToSequence = FastaUtils.parseFasta(fastaBytes, headerParser);
		ensureSourceExists(cmdContext, sourceName);
		
		List<String> updatableProperties = fieldParsers.stream().map(fp -> fp.getProperty()).collect(Collectors.toList());
		Map<String, PropertyPathInfo> propertyPathToInfo = 
				SequencePopulator.getPropertyPathToInfoMap(cmdContext, updatableProperties);
		
		idToSequence.forEach((id, seq) -> {
			if(skipExistingSequences && sequenceExists(cmdContext, sourceName, id)) {
				return;
			}
			String sequenceAsString = seq.getSequenceAsString();
			String seqString = ">"+id+"\n"+sequenceAsString+"\n";
			Sequence sequence = createSequence(cmdContext, sourceName, id, SequenceFormat.FASTA, seqString.getBytes());
			Collection<Object> userCollection = seq.getUserCollection();
			userCollection.forEach(obj -> {
				FastaFieldParser.Result result = (Result) obj;
				PropertyPopulator propertyPopulator = result.getPropertyPopulator();
				PropertyPathInfo propertyPathInfo = propertyPathToInfo.get(propertyPopulator.getProperty());
				PropertyUpdate propertyUpdate = PropertyPopulator.generatePropertyUpdate(propertyPathInfo, sequence, propertyPopulator, result.getValue());
				PropertyPopulator.applyUpdateToDB(cmdContext, sequence, propertyUpdate);
			});
		});
		return new CreateResult(Sequence.class, idToSequence.keySet().size());
	}

	
	private class HeaderParser implements SequenceHeaderParserInterface<DNASequence, NucleotideCompound> {

		@Override
		public void parseHeader(String header, DNASequence sequence) {
			String finalID = ValueExtractor.extractValue(FastaImporter.this, header);
			if(finalID == null) {
				throw new FastaImporterException(FastaImporterException.Code.NULL_IDENTIFIER, header);
			}
			sequence.setAccession(new AccessionID(finalID));
			Collection<Object> fieldParserResults = 
					fieldParsers.stream()
					.map(fParser -> fParser.parseField(header))
					.filter(Optional::isPresent)
				    .map(Optional::get)
				    .collect(Collectors.toList());
			sequence.setUserCollection(fieldParserResults);
		}
	}
	
	
	@Override
	public RegexExtractorFormatter getMainExtractor() {
		return mainExtractor;
	}

	@Override
	public List<RegexExtractorFormatter> getValueConverters() {
		return valueConverters;
	}

	@Override
	public Pattern getNullRegex() {
		return nullRegex;
	}

}
