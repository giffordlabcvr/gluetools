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

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public class AaReferenceSegment extends ReferenceSegment implements Plugin, IAaReferenceSegment, Cloneable {
	
	private CharSequence aminoAcids;

	public AaReferenceSegment(int refStart, int refEnd, CharSequence aminoAcids) {
		super(refStart, refEnd);
		setAminoAcids(aminoAcids);
	}
	public AaReferenceSegment(CommandObject commandObject) {
		super(commandObject);
		setAminoAcids(commandObject.getString(AMINO_ACIDS));
	}
	
	public AaReferenceSegment(PluginConfigContext pluginConfigContext, Element configElem) {
		super();
		configure(pluginConfigContext, configElem);
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		setAminoAcids(PluginUtils.configureStringProperty(configElem, AMINO_ACIDS, true));
	}
	
	@Override
	public CharSequence getAminoAcids() {
		return aminoAcids;
	}
	
	public void setAminoAcids(CharSequence aminoAcids) {
		this.aminoAcids = aminoAcids;
	}
	public void truncateLeft(int length) {
		super.truncateLeft(length);
		setAminoAcids(getAminoAcids().subSequence(length, getAminoAcids().length()));
	}

	public void truncateRight(int length) {
		super.truncateRight(length);
		setAminoAcids(getAminoAcids().subSequence(0, getAminoAcids().length() - length));
	}

	public AaReferenceSegment clone() {
		return new AaReferenceSegment(getRefStart(), getRefEnd(), aminoAcids);
	}

	
	
	public String toString() { return
		super.toString() +
				" AAs: "+getAminoAcids();
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + aminoAcids.hashCode();
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
		AaReferenceSegment other = (AaReferenceSegment) obj;
		if (!aminoAcids.equals(other.aminoAcids))
			return false;
		if (getRefEnd() != other.getRefEnd())
			return false;
		if (getRefStart() != other.getRefStart())
			return false;
		return true;
	}

	public void toDocument(CommandObject builder) {
		super.toDocument(builder);
		builder.set(AMINO_ACIDS, getAminoAcids());
	}
	
	
}