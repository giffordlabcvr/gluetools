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
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;

public abstract class ExtendedSamReporterCommand<R extends CommandResult> extends BaseSamReporterCommand<R> {

	public static final String MIN_Q_SCORE = "minQScore";
	public static final String MIN_MAP_Q = "minMapQ";
	public static final String MIN_DEPTH = "minDepth";
	
	public static final String SAM_REF_SENSE = "samRefSense";

	private Optional<Integer> minQScore;
	private Optional<Integer> minDepth;
	private Optional<Integer> minMapQ;
	
	private Optional<SamRefSense> samRefSense;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.minQScore = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MIN_Q_SCORE, 0, true, 99, true, false));
		this.minMapQ = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MIN_MAP_Q, 0, true, 99, true, false));
		this.minDepth = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MIN_DEPTH, 0, true, null, false, false));
		this.samRefSense = Optional.ofNullable(PluginUtils.configureEnumProperty(SamRefSense.class, configElem, SAM_REF_SENSE, false));
	}

	public int getMinQScore(SamReporter samReporter) {
		return minQScore.orElse(samReporter.getDefaultMinQScore());
	}

	public int getMinDepth(SamReporter samReporter) {
		return minDepth.orElse(samReporter.getDefaultMinDepth());
	}

	public int getMinMapQ(SamReporter samReporter) {
		return minMapQ.orElse(samReporter.getDefaultMinMapQ());
	}

	public SamRefSense getSamRefSense(SamReporter samReporter) {
		return samRefSense.orElse(samReporter.getDefaultSamRefSense());
	}
	
	protected Optional<Integer> getSuppliedMinQScore() {
		return minQScore;
	}

	protected Optional<Integer> getSuppliedMinDepth() {
		return minDepth;
	}

	protected Optional<Integer> getSuppliedMinMapQ() {
		return minMapQ;
	}
	

	public int getConsensusMinDepth(SamReporter samReporter) {
		return samReporter.getConsensusMinDepth();
	}

	public int getConsensusMinQScore(SamReporter samReporter) {
		return samReporter.getConsensusMinQScore();
	}

	public int getConsensusMinMapQ(SamReporter samReporter) {
		return samReporter.getConsensusMinMapQ();
	}
	

	public static class Completer extends BaseSamReporterCommand.Completer {

		public Completer() {
			super();
			registerEnumLookup("samRefSense", SamRefSense.class);
		}
		
	}

}
