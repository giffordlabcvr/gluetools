package uk.ac.gla.cvr.gluetools.core.command.project;

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

@CommandClass( 
		commandWords={"concatenate", "sequence"}, 
		docoptUsages={
			"[-g <gapChars>] <newSourceName> <newSequenceID> <sourceName1> <sequenceID1> <sourceName2> <sequenceID2>"
		}, 
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
			"-g <gapChars>, --gapChars <gapChars>  Integer, number of N characters to insert between sequences"},
		description="Create a new sequence by concatenating the nucleotides of two existing sequences") 
public class ConcatenateSequenceCommand extends ProjectModeCommand<CreateResult> {

	private Integer gapChars; 
	private String newSourceName; 
	private String newSequenceID; 
	private String sourceName1; 
	private String sequenceID1; 
	private String sourceName2; 
	private String sequenceID2;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.gapChars = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "gapChars", false)).orElse(0);
		this.newSourceName = PluginUtils.configureStringProperty(configElem, "newSourceName", true);
		this.newSequenceID = PluginUtils.configureStringProperty(configElem, "newSequenceID", true);
		this.sourceName1 = PluginUtils.configureStringProperty(configElem, "sourceName1", true);
		this.sequenceID1 = PluginUtils.configureStringProperty(configElem, "sequenceID1", true);
		this.sourceName2 = PluginUtils.configureStringProperty(configElem, "sourceName2", true);
		this.sequenceID2 = PluginUtils.configureStringProperty(configElem, "sequenceID2", true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		Sequence sequence1 = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName1, sequenceID1));
		Sequence sequence2 = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName2, sequenceID2));
		
		String nucleotides1 = sequence1.getSequenceObject().getNucleotides(cmdContext);
		String nucleotides2 = sequence2.getSequenceObject().getNucleotides(cmdContext);
		
		StringBuffer nts = new StringBuffer();
		nts.append(nucleotides1);
		for(int i = 0; i < gapChars; i++) {
			nts.append('N');
		}
		nts.append(nucleotides2);
		Sequence sequence = CreateSequenceCommand.createSequence(cmdContext, newSourceName, newSequenceID, false);
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(newSourceName));
		sequence.setSource(source);
		sequence.setFormat(SequenceFormat.FASTA.name());
		sequence.setOriginalData(FastaUtils.seqIdCompoundsPairToFasta(newSequenceID, nts.toString()).getBytes());
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
		}

		private void registerSourceSeqPair(String sourceNameKey, String seqIdKey) {
			registerDataObjectNameLookup(sourceNameKey, Source.class, Source.NAME_PROPERTY);
			registerVariableInstantiator(seqIdKey, new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return AdvancedCmdCompleter.listNames(cmdContext, prefix, Sequence.class, Sequence.SEQUENCE_ID_PROPERTY, 
							ExpressionFactory.matchExp(Sequence.SOURCE_NAME_PATH, bindings.get(sourceNameKey)));
				}
			});
		}
	}
	
}