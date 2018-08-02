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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.ExpressionFactory;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@CommandClass( 
		commandWords={"concatenate", "sequence"}, 
		docoptUsages={
			"[-g <gapChars>] <newSourceName> <newSequenceID> <sourceName1> <sequenceID1> <sourceName2> <sequenceID2> "+
					"[<sourceName3> <sequenceID3>] "+ 
					"[<sourceName4> <sequenceID4>] "+ 
					"[<sourceName5> <sequenceID5>] "+ 
					"[<sourceName6> <sequenceID6>] "+ 
					"[<sourceName7> <sequenceID7>] "+ 
					"[<sourceName8> <sequenceID8>] "+ 
					"[<sourceName9> <sequenceID9>] "+ 
					"[<sourceName10> <sequenceID10>]"
		}, 
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
			"-g <gapChars>, --gapChars <gapChars>  Integer, number of N characters to insert between sequences"},
		description="Create a new sequence by concatenating the nucleotides of two or more existing sequences") 
public class ConcatenateSequenceCommand extends ProjectModeCommand<CreateResult> {

	private Integer gapChars; 
	private String newSourceName; 
	private String newSequenceID; 
	private List<Map<String, String>> inputSequencePkMaps;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.gapChars = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "gapChars", false)).orElse(0);
		this.newSourceName = PluginUtils.configureStringProperty(configElem, "newSourceName", true);
		this.newSequenceID = PluginUtils.configureStringProperty(configElem, "newSequenceID", true);
		String sourceName1 = PluginUtils.configureStringProperty(configElem, "sourceName1", true);
		String sequenceID1 = PluginUtils.configureStringProperty(configElem, "sequenceID1", true);
		String sourceName2 = PluginUtils.configureStringProperty(configElem, "sourceName2", true);
		String sequenceID2 = PluginUtils.configureStringProperty(configElem, "sequenceID2", true);
		
		this.inputSequencePkMaps = new ArrayList<Map<String, String>>();
		this.inputSequencePkMaps.add(Sequence.pkMap(sourceName1, sequenceID1));
		this.inputSequencePkMaps.add(Sequence.pkMap(sourceName2, sequenceID2));
		
		boolean populated = true;
		
		for(int i = 3; i <= 10; i++) {
			String sourceName_i = PluginUtils.configureStringProperty(configElem, "sourceName"+i, false);
			String sequenceID_i = PluginUtils.configureStringProperty(configElem, "sequenceID"+i, false);
			if(sourceName_i == null || sequenceID_i == null) {
				populated = false;
			}
			if(populated == false) {
				if(sourceName_i != null || sequenceID_i != null) {
					throw new CommandException(Code.COMMAND_USAGE_ERROR, "Non-consecutive or missing sourceName / sequenceID arguments");
				}
			}
			if(sourceName_i != null && sequenceID_i != null) {
				this.inputSequencePkMaps.add(Sequence.pkMap(sourceName_i, sequenceID_i));
			}
		}
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		StringBuffer nts = new StringBuffer();

		boolean first = true;
		for(Map<String, String> pkMap: inputSequencePkMaps) {
			if(!first) {
				for(int i = 0; i < gapChars; i++) {
					nts.append('N');
				}
			}
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, pkMap);
			String nucleotides = sequence.getSequenceObject().getNucleotides(cmdContext);
			nts.append(nucleotides);
			first = false;
		}

		Sequence sequence = CreateSequenceCommand.createSequence(cmdContext, newSourceName, newSequenceID, false);
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(newSourceName));
		sequence.setSource(source);
		sequence.setFormat(SequenceFormat.FASTA.name());
		sequence.setOriginalData(FastaUtils.seqIdCompoundsPairToFasta(newSequenceID, nts.toString(), LineFeedStyle.LF).getBytes());
		cmdContext.commit();
		return new CreateResult(Sequence.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerSourceSeqPair("newSourceName", "newSequenceID");
			registerSourceSeqPair("sourceName1", "sequenceID1");
			registerSourceSeqPair("sourceName2", "sequenceID2");
			registerSourceSeqPair("sourceName3", "sequenceID3");
			registerSourceSeqPair("sourceName4", "sequenceID4");
			registerSourceSeqPair("sourceName5", "sequenceID5");
			registerSourceSeqPair("sourceName6", "sequenceID6");
			registerSourceSeqPair("sourceName7", "sequenceID7");
			registerSourceSeqPair("sourceName8", "sequenceID8");
			registerSourceSeqPair("sourceName9", "sequenceID9");
			registerSourceSeqPair("sourceName10", "sequenceID10");
		}

		private void registerSourceSeqPair(String sourceNameKey, String seqIdKey) {
			registerDataObjectNameLookup(sourceNameKey, Source.class, Source.NAME_PROPERTY);
			registerVariableInstantiator(seqIdKey, new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return AdvancedCmdCompleter.listNames(cmdContext, prefix, Sequence.class, Sequence.SEQUENCE_ID_PROPERTY, 
							ExpressionFactory.matchExp(Sequence.SOURCE_NAME_PATH, bindings.get(sourceNameKey)));
				}
			});
		}
	}
	
}
