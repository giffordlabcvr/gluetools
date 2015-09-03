package uk.ac.gla.cvr.gluetools.core.segments;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public class NtReferenceSegment extends ReferenceSegment implements Plugin, INtReferenceSegment, Cloneable {
	
	public static final String NUCLEOTIDES = "nucleotides";

	private CharSequence nucleotides;

	public NtReferenceSegment(int refStart, int refEnd, CharSequence nucleotides) {
		super(refStart, refEnd);
		setNucleotides(nucleotides);
	}
	public NtReferenceSegment(ObjectReader objectReader) {
		super(objectReader);
		setNucleotides(objectReader.stringValue(NUCLEOTIDES));
	}
	
	public NtReferenceSegment(PluginConfigContext pluginConfigContext, Element configElem) {
		super();
		configure(pluginConfigContext, configElem);
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		setNucleotides(PluginUtils.configureStringProperty(configElem, NUCLEOTIDES, true));
	}
	
	@Override
	public CharSequence getNucleotides() {
		return nucleotides;
	}
	
	public void setNucleotides(CharSequence nucleotides) {
		this.nucleotides = nucleotides;
	}

	public void truncateLeft(int length) {
		super.truncateLeft(length);
		setNucleotides(getNucleotides().subSequence(length, getNucleotides().length()));
	}

	public void truncateRight(int length) {
		super.truncateRight(length);
		setNucleotides(getNucleotides().subSequence(0, getNucleotides().length() - length));
	}

	public NtReferenceSegment clone() {
		return new NtReferenceSegment(getRefStart(), getRefEnd(), nucleotides);
	}

	
	public String toString() { return
		super.toString() +
				" NTs: "+getNucleotides();
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + nucleotides.hashCode();
		result = prime * result + getRefEnd();
		result = prime * result + getRefStart();
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NtReferenceSegment other = (NtReferenceSegment) obj;
		if (!nucleotides.equals(other.nucleotides))
			return false;
		if (getRefEnd() != other.getRefEnd())
			return false;
		if (getRefStart() != other.getRefStart())
			return false;
		return true;
	}

	public void toDocument(ObjectBuilder builder) {
		super.toDocument(builder);
		builder
			.set(NUCLEOTIDES, getNucleotides());
	}
	
}