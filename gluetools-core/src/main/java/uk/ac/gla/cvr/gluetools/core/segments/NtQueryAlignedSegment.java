package uk.ac.gla.cvr.gluetools.core.segments;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class NtQueryAlignedSegment extends QueryAlignedSegment implements INtReferenceSegment {

	private CharSequence nucleotides;
	
	public NtQueryAlignedSegment(int refStart, int refEnd, int queryStart, int queryEnd, CharSequence nucleotides) {
		super(refStart, refEnd, queryStart, queryEnd);
		this.nucleotides = nucleotides;
	}

	public NtQueryAlignedSegment(ObjectReader objectReader) {
		super(objectReader);
		setNucleotides(objectReader.stringValue(NUCLEOTIDES));
	}

	public NtQueryAlignedSegment(PluginConfigContext pluginConfigContext, Element configElem) {
		super(pluginConfigContext, configElem);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		setNucleotides(PluginUtils.configureStringProperty(configElem, NUCLEOTIDES, true));
	}

	@Override
	public void toDocument(ObjectBuilder builder) {
		super.toDocument(builder);
		builder.set(NUCLEOTIDES, getNucleotides());
	}

	@Override
	public CharSequence getNucleotides() {
		return nucleotides;
	}

	public void setNucleotides(CharSequence nucleotides) {
		this.nucleotides = nucleotides;
	}

	public String toString() { return
			super.toString() + " NTs: "+getNucleotides();
	}
	
	public NtQueryAlignedSegment clone() {
		return new NtQueryAlignedSegment(getRefStart(), getRefEnd(), getQueryStart(), getQueryEnd(), getNucleotides());
	}

	@Override
	public void truncateLeft(int length) {
		super.truncateLeft(length);
		setNucleotides(getNucleotides().subSequence(length, getNucleotides().length()));
	}

	@Override
	public void truncateRight(int length) {
		super.truncateRight(length);
		setNucleotides(getNucleotides().subSequence(0, getNucleotides().length() - length));
	}

	@Override
	public int ntIndexAtRefLoction(int refLocation) {
		int refToQueryOffset = getRefStart() - getQueryStart();
		return refLocation+refToQueryOffset;
	}
	
}
