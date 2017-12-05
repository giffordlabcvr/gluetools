package uk.ac.gla.cvr.gluetools.core.plugins;


public abstract class PluginGroup implements Comparable<PluginGroup> {

	private String id;
	private String description;
	private int orderingKey;

	public PluginGroup(String id, String description, int orderingKey) {
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
		PluginGroup other = (PluginGroup) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public int compareTo(PluginGroup o) {
		return Integer.compare(this.orderingKey, o.orderingKey);
	}

	
}
