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
