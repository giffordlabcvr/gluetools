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
package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHspFilter;

public class RecognitionCategory implements Plugin {

	
	private static final String ID = "id";
	private static final String DISPLAY_NAME = "displayName";
	private static final String MINIMUM_SCORE = "minimumScore";
	private static final String MAXIMUM_E_VALUE = "maximumEValue";
	private static final String MINIMUM_BIT_SCORE = "minimumBitScore";
	private static final String MINIMUM_IDENTITY_PCT = "minimumIdentityPct";
	private static final String MINIMUM_TOTAL_ALIGN_LENGTH = "minimumTotalAlignLength";
	private static final String REFERENCE_SEQUENCE = "referenceSequence";
	
	private String id;
	private List<String> refSeqNames;
	private String displayName;
	private Optional<Double> maximumEValue;
	private Optional<Double> minimumBitScore;
	private Optional<Double> minimumIdentityPct;
	private Optional<Integer> minimumScore;
	private Integer minimumTotalAlignLength;
	
	public RecognitionCategory() {
		super();
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.id = PluginUtils.configureStringProperty(configElem, ID, true);
		this.refSeqNames = PluginUtils.configureStringsProperty(configElem, REFERENCE_SEQUENCE, 1, null);
		this.displayName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DISPLAY_NAME, false)).orElse(this.id);
		this.minimumBitScore = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MINIMUM_BIT_SCORE, false));
		this.minimumIdentityPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MINIMUM_IDENTITY_PCT, false));
		this.maximumEValue = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MAXIMUM_E_VALUE, false));
		this.minimumScore = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MINIMUM_SCORE, false));
		this.minimumTotalAlignLength = Optional.ofNullable(
				PluginUtils.configureIntProperty(configElem, MINIMUM_TOTAL_ALIGN_LENGTH, 2, true, null, false, false)).orElse(10);
	}

	public String getId() {
		return id;
	}

	public List<String> getRefSeqNames() {
		return refSeqNames;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Optional<Double> getMaximumEValue() {
		return maximumEValue;
	}

	public Optional<Double> getMinimumBitScore() {
		return minimumBitScore;
	}

	public Optional<Double> getMinimumIdentityPct() {
		return minimumIdentityPct;
	}

	public Optional<Integer> getMinimumScore() {
		return minimumScore;
	}

	public Integer getMinimumTotalAlignLength() {
		return minimumTotalAlignLength;
	}

	public BlastHspFilter getForwardHspFilter() {
		return new BlastHspFilter() {
			@Override
			public boolean allowBlastHsp(BlastHsp blastHsp) {
				// GlueLogger.getGlueLogger().finest("HSP eValue:"+blastHsp.getEvalue()+" queryFrom "+blastHsp.getQueryFrom()+" queryTo "+blastHsp.getQueryTo()+" hitFrom "+blastHsp.getHitFrom()+" hitTo "+blastHsp.getHitTo());
				return blastHsp.getQueryTo() >= blastHsp.getQueryFrom() &&
					blastHsp.getHitTo() >= blastHsp.getHitFrom() &&
							getMaximumEValue().map(maxEValue -> (maxEValue >= blastHsp.getEvalue())).orElse(true)&&
					getMinimumBitScore().map(minBitScore -> (minBitScore <= blastHsp.getBitScore())).orElse(true)&&
					getMinimumScore().map(minScore -> (minScore <= blastHsp.getScore())).orElse(true)&&
					getMinimumIdentityPct().map(minIdentityPct -> (minIdentityPct <= blastHsp.getIdentityPct())).orElse(true);
			}
		};
	}

	public BlastHspFilter getReverseHspFilter() {
		return new BlastHspFilter() {
			@Override
			public boolean allowBlastHsp(BlastHsp blastHsp) {
				return (blastHsp.getQueryTo() < blastHsp.getQueryFrom()) ||
						(blastHsp.getHitTo() < blastHsp.getHitFrom()) &&
						getMaximumEValue().map(maxEValue -> (maxEValue >= blastHsp.getEvalue())).orElse(true)&&
						getMinimumBitScore().map(minBitScore -> (minBitScore <= blastHsp.getBitScore())).orElse(true)&&
						getMinimumScore().map(minScore -> (minScore <= blastHsp.getScore())).orElse(true)&&
						getMinimumIdentityPct().map(minIdentityPct -> (minIdentityPct <= blastHsp.getIdentityPct())).orElse(true);
			}
		};
	}

}
