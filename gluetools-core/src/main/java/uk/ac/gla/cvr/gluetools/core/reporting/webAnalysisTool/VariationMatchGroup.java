package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class VariationMatchGroup {

	@PojoDocumentField
	public String referenceName;

	@PojoDocumentField
	public String featureName;

	@PojoDocumentField
	public String variationCategory;

	@PojoDocumentListField(itemClass = VariationMatch.class)
	public List<VariationMatch> variationMatch = new ArrayList<VariationMatch>();
	
	public static class Key {
		private String referenceName;
		private String featureName;
		private String variationCategory;
		
		public Key(String referenceName, String featureName, String variationCategory) {
			super();
			this.referenceName = referenceName;
			this.featureName = featureName;
			this.variationCategory = variationCategory;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((featureName == null) ? 0 : featureName.hashCode());
			result = prime * result
					+ ((referenceName == null) ? 0 : referenceName.hashCode());
			result = prime
					* result
					+ ((variationCategory == null) ? 0 : variationCategory
							.hashCode());
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
			Key other = (Key) obj;
			if (featureName == null) {
				if (other.featureName != null)
					return false;
			} else if (!featureName.equals(other.featureName))
				return false;
			if (referenceName == null) {
				if (other.referenceName != null)
					return false;
			} else if (!referenceName.equals(other.referenceName))
				return false;
			if (variationCategory == null) {
				if (other.variationCategory != null)
					return false;
			} else if (!variationCategory.equals(other.variationCategory))
				return false;
			return true;
		}
		
		
		
	}
	
}
