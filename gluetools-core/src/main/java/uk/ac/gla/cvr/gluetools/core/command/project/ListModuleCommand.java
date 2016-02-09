package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;


@CommandClass( 
	commandWords={"list", "module"}, 
	docoptUsages={""},
	description="List modules") 
public class ListModuleCommand extends ProjectModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		List<Module> modules = GlueDataObject.query(cmdContext, Module.class, new SelectQuery(Module.class));
		return new ListResult(Module.class, modules, Arrays.asList("name", "type"), 
				new BiFunction<Module, String, Object>() {
					@Override
					public Object apply(Module t, String u) {
						if(u.equals("name")) {
							return t.getName();
						} else if(u.equals("type")) {
							return t.getType();
						}
						return null;
					}
		});
	}

}
