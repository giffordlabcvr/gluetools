package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"show", "ancestors"}, 
		description="Show the alignment's ancestors",
		docoptUsages={"[-t <toAlmtName>]"},
		docoptOptions={"-t <toAlmtName>, --toAlmtName <toAlmtName>  Stop at specific alignment"},
		metaTags={}
	) 
public class AlignmentShowAncestorsCommand extends AlignmentModeCommand<AlignmentShowAncestorsCommand.ShowAlignmentAncestorsResult>{

	public static final String TO_ALMT_NAME = "toAlmtName";
	private Optional<String> toAlmtName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		toAlmtName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, TO_ALMT_NAME, false));
	}


	@Override
	public ShowAlignmentAncestorsResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		String thisAlmtName = getAlignmentName();
		Alignment thisAlmt = GlueDataObject.lookup(objContext, Alignment.class, Alignment.pkMap(thisAlmtName));
		Alignment toAlmt = toAlmtName
				.map(name -> GlueDataObject.lookup(objContext, Alignment.class, Alignment.pkMap(name)))
				.orElse(null);
		List<Alignment> ancestors = new ArrayList<Alignment>();
		Alignment currentAlmt = thisAlmt.getParent();
		while(currentAlmt != null) {
			ancestors.add(currentAlmt);
			if(toAlmt != null && currentAlmt.getName().equals(toAlmt.getName())) {
				break;
			}
			currentAlmt = currentAlmt.getParent();
		}
		return new ShowAlignmentAncestorsResult(ancestors);
	}

	
	public static class ShowAlignmentAncestorsResult extends TableResult {

		public ShowAlignmentAncestorsResult(List<Alignment> ancestorAlignments) {
			super("showAlignmentAncestors", Arrays.asList(Alignment.NAME_PROPERTY), 
					ancestorAlignments.stream()
					.map(almt -> Collections.singletonMap(Alignment.NAME_PROPERTY, (Object) almt.getName()))
					.collect(Collectors.toList()));
		}

	}


}
