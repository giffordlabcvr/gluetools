package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.function.BiFunction;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class NtQueryAlignedSegment extends QueryAlignedSegment implements INtTranscribableSegment {

	private CharSequence nucleotides;
	
	public NtQueryAlignedSegment(int refStart, int refEnd, int queryStart, int queryEnd, CharSequence nucleotides) {
		super(refStart, refEnd, queryStart, queryEnd);
		this.nucleotides = nucleotides;
	}

	public NtQueryAlignedSegment(CommandObject commandObject) {
		super(commandObject);
		setNucleotides(commandObject.getString(NUCLEOTIDES));
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
	public void toDocument(CommandObject builder) {
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
	
	public static BiFunction<NtQueryAlignedSegment, NtQueryAlignedSegment, NtQueryAlignedSegment> ntMergeAbuttingFunction() {
		return (seg1, seg2) -> {
			String nucleotides = seg1.getNucleotides().toString() + seg2.getNucleotides().toString();
			return new NtQueryAlignedSegment(seg1.getRefStart(), seg2.getRefEnd(), seg1.getQueryStart(), seg2.getQueryEnd(), nucleotides);
		};

	}

	
}
