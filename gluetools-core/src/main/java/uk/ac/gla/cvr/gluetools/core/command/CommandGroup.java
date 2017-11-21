package uk.ac.gla.cvr.gluetools.core.command;

public class CommandGroup implements Comparable<CommandGroup> {

	public static final CommandGroup 
		MODE_NAVIGATION = new CommandGroup("navigation", "Commands for navigation between command modes", 0),
		MISC = new CommandGroup("miscellaneous", "Miscellaneous commands", 90);
	
	private String id;
	private String description;
	private int orderingKey;

	public CommandGroup(String id, String description, int orderingKey) {
		super();
		this.id = id;
		this.description = description;
		this.orderingKey = orderingKey;
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
	
	
}
