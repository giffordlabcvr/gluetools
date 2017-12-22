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
