package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;



public class TestXmlPopulator {
	/*
	
	String 
		GB_GI_NUMBER = "GB_GI_NUMBER",
		GB_PRIMARY_ACCESSION = "GB_PRIMARY_ACCESSION",
		GB_ACCESSION_VERSION = "GB_ACCESSION_VERSION",
		GB_LOCUS = "GB_LOCUS",
		GB_LENGTH = "GB_LENGTH",
		GB_GENOTYPE = "GB_GENOTYPE",
		GB_SUBTYPE = "GB_SUBTYPE",
		GB_RECOMBINANT = "GB_RECOMBINANT",
		GB_PATENT_RELATED = "GB_PATENT_RELATED",
		GB_ORGANISM = "GB_ORGANISM",
		GB_ISOLATE = "GB_ISOLATE",
		GB_TAXONOMY = "GB_TAXONOMY",
		GB_HOST = "GB_HOST", 
		GB_COUNTRY = "GB_COUNTRY",
		GB_COLLECTION_YEAR = "GB_COLLECTION_YEAR", 
		GB_COLLECTION_MONTH = "GB_COLLECTION_MONTH",
		GB_COLLECTION_MONTH_DAY = "GB_COLLECTION_MONTH_DAY",
		GB_CREATE_DATE = "GB_CREATE_DATE",
		GB_UPDATE_DATE = "GB_UPDATE_DATE";
	
	

	//@Test 
	public void testHcvRuleSet() throws Exception {
		String xmlDirectory = "/Users/joshsinger/hcv_rega/retrieved_xml";
		String populatorRulesFile = "hcvRuleSet.xml";
		
		
		List<FieldTranslator<?>> fields = Arrays.asList(new FieldTranslator<?>[]{
				new StringFieldTranslator(GB_GI_NUMBER),
				new StringFieldTranslator(GB_PRIMARY_ACCESSION),
				new StringFieldTranslator(GB_ACCESSION_VERSION),
				new StringFieldTranslator(GB_LOCUS),
				new IntegerFieldTranslator(GB_LENGTH),
				new StringFieldTranslator(GB_GENOTYPE), 
				new StringFieldTranslator(GB_SUBTYPE),
				new BooleanFieldTranslator(GB_RECOMBINANT),
				new BooleanFieldTranslator(GB_PATENT_RELATED),
				new StringFieldTranslator(GB_ORGANISM),
				new StringFieldTranslator(GB_ISOLATE),
				new StringFieldTranslator(GB_TAXONOMY),
				new StringFieldTranslator(GB_HOST),
				new StringFieldTranslator(GB_COUNTRY),
				new IntegerFieldTranslator(GB_COLLECTION_YEAR),
				new StringFieldTranslator(GB_COLLECTION_MONTH),
				new IntegerFieldTranslator(GB_COLLECTION_MONTH_DAY),
				new DateFieldTranslator(GB_CREATE_DATE),
				new DateFieldTranslator(GB_UPDATE_DATE),
				
		});
		Project project = initProjectFromFields(fields);
		List<CollatedSequence> collatedSequences = initSequencesXml(project, xmlDirectory);
		runPopulator(collatedSequences, populatorRulesFile);
		@SuppressWarnings("unused")
		Predicate<? super CollatedSequence> problematicPredicate = problematicPredicate();
		
		//collatedSequences = collatedSequences.stream().filter(problematicPredicate).collect(Collectors.toList());
		//List<String> displayFieldNames = fields.stream().map(s -> s.getName()).collect(Collectors.toList());
		List<String> displayFieldNames = Arrays.asList(new String[]{
//				GB_GI_NUMBER,
//				GB_PRIMARY_ACCESSION, 
//				GB_ACCESSION_VERSION,
//				GB_LOCUS,
//				GB_LENGTH,
				GB_GENOTYPE,
				GB_SUBTYPE,
//				GB_RECOMBINANT, 
//				GB_PATENT_RELATED,
//				GB_ORGANISM,
//				GB_ISOLATE,
//				GB_TAXONOMY,
//				GB_HOST, 
//				GB_COUNTRY, 
//				GB_COLLECTION_YEAR,
//				GB_COLLECTION_MONTH,
//				GB_COLLECTION_MONTH_DAY,
//				GB_CREATE_DATE,
//				GB_UPDATE_DATE,
		});
		dumpFieldValues(displayFieldNames, collatedSequences);
	}

	// return true if the sequence fields are problematic.
	private Predicate<? super CollatedSequence> problematicPredicate() {
		return seq -> {
			if(!seq.getString(GB_ORGANISM).equals(Optional.of("Hepatitis C virus"))) {
				return false;
			}
			if(seq.getBoolean(GB_PATENT_RELATED).equals(Optional.of(Boolean.TRUE))) {
				if(seq.hasFieldValue(GB_GENOTYPE) || seq.hasFieldValue(GB_SUBTYPE)) {
					return true;
				} else {
					return false;
				}
			}
			if(seq.getBoolean(GB_RECOMBINANT).equals(Optional.of(Boolean.TRUE))) {
				return false;
			}
			if(seq.hasFieldValue(GB_GENOTYPE) && seq.hasFieldValue(GB_SUBTYPE)) {
				return false;
			}			
			return true;

		};
	}
	
	private void dumpFieldValues(List<String> fieldNames,
			List<CollatedSequence> collatedSequences) {
		collatedSequences.forEach(sequence -> {
			System.out.print(sequence.getSequenceSourceID()+" -- ");
			fieldNames.forEach(fieldName -> {
				sequence.getFieldValue(fieldName).ifPresent(f -> { System.out.print(fieldName+": "+f+", "); });
			});
			System.out.println();
		});
		System.out.println("------------------\nTotal: "+collatedSequences.size()+" sequences");
		
	}

	private Project initProjectFromFields(List<FieldTranslator<?>> fields) {
		Project project = new Project();
		fields.forEach(f -> {project.addDataField(f);});
		return project;
	}

	
	private void runPopulator(List<CollatedSequence> collatedSequences, String populatorRulesFile)
			throws SAXException, IOException {
		Document document;
		try(InputStream docStream = getClass().getResourceAsStream(populatorRulesFile)) {
			document = XmlUtils.documentFromStream(docStream);
		}
		PluginConfigContext pluginConfigContext = new PluginConfigContext(new Configuration());
		GenbankXmlPopulatorPlugin dataFieldPopulator = (GenbankXmlPopulatorPlugin) PluginFactory.get(ModulePluginFactory.creator).
				createFromElement(pluginConfigContext, document.getDocumentElement());
		collatedSequences.forEach(sequence -> {
			dataFieldPopulator.populate(sequence);
		});
	}

	
	
	public List<CollatedSequence> initSequencesXml(Project project, String directoryPath) {
		File directory = new File(directoryPath);
		File[] files = directory.listFiles();
		return Arrays.asList(files).stream().map(file -> {
			Document document;
			try(FileInputStream fileInputStream = new FileInputStream(file)) {
				document = XmlUtils.documentFromStream(fileInputStream);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
			CollatedSequence collatedSequence = new CollatedSequence();
			collatedSequence.setOwningProject(project);
			collatedSequence.setSequenceSourceID(file.getName().replace(".xml", ""));
			collatedSequence.setFormat(SequenceFormat.GENBANK_XML);
			collatedSequence.setSequenceDocument(document);
			return collatedSequence;
		}).collect(Collectors.toList());
	}
	
	@Test
	public void textXPath() throws Exception {
		Document document = XmlUtils.documentFromStream(getClass().getResourceAsStream("testXmlPopulator1.xml"));
		System.out.println(XmlUtils.getXPathStrings(document, "/dataFieldPopulator/*[self::rules|self::foo]/xPathNodes/xPathExpression/text()"));
	}
	
	*/
}
