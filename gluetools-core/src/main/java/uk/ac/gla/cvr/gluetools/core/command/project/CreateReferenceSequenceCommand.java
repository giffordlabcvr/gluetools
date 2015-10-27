package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","reference"}, 
	docoptUsages={"<refSeqName> <sourceName> <sequenceID>"},
	description="Create a new reference sequence, based on a specific sequence", 
	metaTags={CmdMeta.updatesDatabase},
	furtherHelp="A reference sequence object decorates a sequence with various nucleotide-related metadata."+
	" While a sequence is a reference sequence, the sequence may not be deleted.") 
public class CreateReferenceSequenceCommand extends ProjectModeCommand<CreateResult> {

	public static final String REF_SEQ_NAME = "refSeqName";
	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	
	private String refSeqName;
	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSeqName = PluginUtils.configureStringProperty(configElem, REF_SEQ_NAME, true);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, 
				Sequence.pkMap(sourceName, sequenceID));
		ReferenceSequence refSequence = GlueDataObject.create(cmdContext, ReferenceSequence.class, 
				ReferenceSequence.pkMap(refSeqName), false);
		refSequence.setSequence(sequence);
		refSequence.setCreationTime(System.currentTimeMillis());
		cmdContext.commit();
		return new CreateResult(ReferenceSequence.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
			registerVariableInstantiator("sequenceID", 
					new QualifiedDataObjectNameInstantiator(Sequence.class, Sequence.SEQUENCE_ID_PROPERTY) {
				@Override
				@SuppressWarnings("rawtypes")
				protected void qualifyResults(CommandMode cmdMode,
						Map<String, Object> bindings, Map<String, Object> qualifierValues) {
					qualifierValues.put(Sequence.SOURCE_NAME_PATH, bindings.get("sourceName"));
				}
			});
		}
	}
}
