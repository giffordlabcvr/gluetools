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
package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.seqOrigData.SeqOrigData;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseCreateSequenceCommand extends ProjectModeCommand<CreateResult> {

	
	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";

	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, true);
	}

	protected CreateResult executeCreateSequence(CommandContext cmdContext, SequenceFormat format, byte[] originalData) {
		Sequence sequence = createSequence(cmdContext, sourceName, sequenceID, false);
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName));
		sequence.setSource(source);
		sequence.setFormat(format.name());
		sequence.setOriginalData(originalData);
		cmdContext.commit();
		return new CreateResult(Sequence.class, 1);
	}
	
	protected String getSequenceID() {
		return sequenceID;
	}

	public static Sequence createSequence(CommandContext cmdContext, String sourceName, String sequenceID, boolean allowExists) {
		Sequence sequence = GlueDataObject.create(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), allowExists);
		SeqOrigData seqOrigData = GlueDataObject.create(cmdContext, SeqOrigData.class, SeqOrigData.pkMap(sourceName, sequenceID), allowExists);
		sequence.setSeqOrigData(seqOrigData);
		seqOrigData.setSequence(sequence);
		return sequence;
	}
}
