package uk.ac.gla.cvr.gluetools.core.collation.sourcing;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerFactoryException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.ncbi.NCBISequenceSourcer;

public class SequenceSourcerFactory {

	private static SequenceSourcerFactory instance;
	
	private Map<String, Class<? extends SequenceSourcer>> typeStringToSourcerClass = 
			new LinkedHashMap<String, Class<? extends SequenceSourcer>>();
	
	private SequenceSourcerFactory() {
		typeStringToSourcerClass.put(NCBISequenceSourcer.TYPE, NCBISequenceSourcer.class);
	}
	
	public static SequenceSourcerFactory getInstance() {
		if(instance == null) {
			instance = new SequenceSourcerFactory();
		}
		return instance;
	}
	
	
	public SequenceSourcer createFromXml(Element element) throws SequenceSourcerFactoryException {
		if(!element.getNodeName().equals("sequenceSourcer")) {
			throw new SequenceSourcerFactoryException(Code.SOURCER_XML_MISSING_ELEMENT, "sequenceSourcer");
		}
		String type = element.getAttribute("type");
		if(type == null) {
			throw new SequenceSourcerFactoryException(Code.SOURCER_XML_MISSING_ATTRIBUTE, "sequenceSourcer", "type");
		}
		Class<? extends SequenceSourcer> sourcerClass = typeStringToSourcerClass.get(type);
		if(sourcerClass == null) {
			throw new SequenceSourcerFactoryException(Code.UNKNOWN_SOURCER_TYPE, type);
		}
		SequenceSourcer sequenceSourcer = null;
		try {
			sequenceSourcer = sourcerClass.newInstance();
		} catch(Exception e) {
			throw new SequenceSourcerFactoryException(e, Code.SOURCER_CREATION_FAILED, sourcerClass.getCanonicalName());
		}
		try {
			sequenceSourcer.configure(element);
		} catch(Exception e) {
			throw new SequenceSourcerFactoryException(e, Code.SOURCER_CONFIGURATION_FAILED, sourcerClass.getCanonicalName());
		}
		return sequenceSourcer;
	}
	
}
