package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
		Alignment currentAlmt = thisAlmt;
		do {
			ancestors.add(currentAlmt);
			if(toAlmt != null && currentAlmt.getName().equals(toAlmt.getName())) {
				break;
			}
			currentAlmt = currentAlmt.getParent();
		} while(currentAlmt != null);
		return new ShowAlignmentAncestorsResult(ancestors);
	}

	
	public static class ShowAlignmentAncestorsResult extends TableResult {

		public ShowAlignmentAncestorsResult(List<Alignment> ancestorAlignments) {
			super("showAlignmentAncestors", Arrays.asList(Alignment.NAME_PROPERTY, Alignment.REF_SEQ_NAME_PATH), 
					ancestorAlignments.stream()
					.map(almt -> {
						Map<String, Object> map = new LinkedHashMap<String, Object>();
						map.put(Alignment.NAME_PROPERTY, almt.readNestedProperty(Alignment.NAME_PROPERTY));
						map.put(Alignment.REF_SEQ_NAME_PATH, almt.readNestedProperty(Alignment.REF_SEQ_NAME_PATH));
						return map;
					})
					.collect(Collectors.toList()));
		}

	}


}
