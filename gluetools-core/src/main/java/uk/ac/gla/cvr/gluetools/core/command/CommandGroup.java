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
package uk.ac.gla.cvr.gluetools.core.command;

public class CommandGroup implements Comparable<CommandGroup> {

	public static final CommandGroup 
		MODE_NAVIGATION = new CommandGroup("navigation", "Command mode navigation", 0, false),
		RENDERING = new CommandGroup("rendering", "Commands for rendering a document from the data object", 55, false),
		VALIDATION = new CommandGroup("validation", "Commands for validating the configuration of the object", 56, false),
		OTHER = new CommandGroup("other", "Other commands", 100, false);
	
	private String id;
	private String description;
	private int orderingKey;
	private boolean nonModeSpecific;

	public CommandGroup(String id, String description, int orderingKey, boolean nonModeSpecific) {
		super();
		this.id = id;
		this.description = description;
		this.orderingKey = orderingKey;
		this.nonModeSpecific = nonModeSpecific;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		CommandGroup other = (CommandGroup) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public int compareTo(CommandGroup o) {
		return Integer.compare(this.orderingKey, o.orderingKey);
	}

	public boolean isNonModeSpecific() {
		return nonModeSpecific;
	}
	
	
}
