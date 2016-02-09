package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass( 
		commandWords={"validate"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Validate that a project is correctly defined.", 
		furtherHelp="Currently, this validates the project's reference sequences, features and modules") 
public class ProjectValidateCommand extends ProjectModeCommand<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		List<Feature> features = 
				GlueDataObject.query(cmdContext, Feature.class, new SelectQuery(Feature.class));
		features.forEach(feature -> feature.validate(cmdContext));
		List<ReferenceSequence> refSeqs = 
				GlueDataObject.query(cmdContext, ReferenceSequence.class, new SelectQuery(ReferenceSequence.class));
		refSeqs.forEach(refSeq -> refSeq.validate(cmdContext));
		List<Module> modules = 
				GlueDataObject.query(cmdContext, Module.class, new SelectQuery(Module.class));
		modules.forEach(module -> module.validate(cmdContext));
		return new OkResult();
	}
	
}

