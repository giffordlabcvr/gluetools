package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

public class RecognitionCategoryResult {

	public enum Direction {
		FORWARD,
		REVERSE
	}
	
	private String categoryId;
	private Direction direction;
	
	public RecognitionCategoryResult(String categoryId, Direction direction) {
		super();
		this.categoryId = categoryId;
		this.direction = direction;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public Direction getDirection() {
		return direction;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((categoryId == null) ? 0 : categoryId.hashCode());
		result = prime * result
				+ ((direction == null) ? 0 : direction.hashCode());
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
		RecognitionCategoryResult other = (RecognitionCategoryResult) obj;
		if (categoryId == null) {
			if (other.categoryId != null)
				return false;
		} else if (!categoryId.equals(other.categoryId))
			return false;
		if (direction != other.direction)
			return false;
		return true;
	}

}
