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

