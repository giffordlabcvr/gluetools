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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;

public abstract class BaseSamReporterCommand<R extends CommandResult> extends ModulePluginCommand<R, SamReporter> {

	public static final String FILE_NAME = "fileName";
	public static final String SAM_REF_NAME = "samRefName";

	private String fileName;
	private String samRefName;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.samRefName = PluginUtils.configureStringProperty(configElem, SAM_REF_NAME, false);
	}

	public String getFileName() {
		return fileName;
	}

	public String getSuppliedSamRefName() {
		return samRefName;
	}

	protected static class SamRefInfo {
		private int samRefIndex;
		private String samRefName;
		private int samRefLength;

		public SamRefInfo(int samRefIndex, String samRefName, int samRefLength) {
			super();
			this.samRefIndex = samRefIndex;
			this.samRefName = samRefName;
			this.samRefLength = samRefLength;
		}

		public int getSamRefIndex() {
			return samRefIndex;
		}

		public String getSamRefName() {
			return samRefName;
		}

		public int getSamRefLength() {
			return samRefLength;
		}
	}
	
	protected SamRefInfo getSamRefInfo(ConsoleCommandContext consoleCmdContext, SamReporter samReporter) {
		String samRefName;
		int samRefLength;
		int samRefIndex;
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), 
				samReporter.getSamReaderValidationStringency())) {
			samRefName = SamUtils.findReference(samReader, getFileName(), getSuppliedSamRefName()).getSequenceName();
	        SAMSequenceRecord samReference = samReader.getFileHeader().getSequenceDictionary().getSequence(samRefName);
	        samRefLength = samReference.getSequenceLength();
	        samRefIndex = samReference.getSequenceIndex();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new SamRefInfo(samRefIndex, samRefName, samRefLength);
	}

	protected static class SamRefNameInstantiator extends AdvancedCmdCompleter.VariableInstantiator {
		@SuppressWarnings("rawtypes")
		@Override
		public List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			String samBamPath = (String) bindings.get("fileName");
			if(samBamPath == null) {
				return null;
			}
			CommandMode<?> commandMode = cmdContext.peekCommandMode();
			if(!(commandMode instanceof ModuleMode)) {
				return null;
			}
			ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
			try {
				String moduleName = ((ModuleMode) commandMode).getModuleName();
				Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
				SamReporter samReporter = (SamReporter) module.getModulePlugin(cmdContext);
				try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, samBamPath, 
						samReporter.getSamReaderValidationStringency())) {
		        	List<SAMSequenceRecord> samSequenceRecords = samReader.getFileHeader().getSequenceDictionary().getSequences();
		        	return samSequenceRecords.stream()
		        			.map(ssr -> new CompletionSuggestion(ssr.getSequenceName(), true))
		        			.collect(Collectors.toList());
				} catch (Exception e) {
					return null;
				}
			} catch(Exception e) {
				return null;
			}
		}
	}

	public static class Completer extends AdvancedCmdCompleter {

		public Completer() {
			super();
			registerPathLookup("fileName", false);
			registerVariableInstantiator("samRefName", new SamRefNameInstantiator());
		}
		
	}

}
