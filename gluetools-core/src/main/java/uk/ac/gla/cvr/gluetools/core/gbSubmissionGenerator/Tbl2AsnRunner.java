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
package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OutputStreamCommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ResultOutputFormat;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.Tbl2AsnException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class Tbl2AsnRunner implements Plugin {

	public static String 
		TBL2ASN_EXECUTABLE_PROPERTY = "gluetools.core.programs.tbl2asn.executable"; 
	public static String 
		TBL2ASN_TEMP_DIR_PROPERTY = "gluetools.core.programs.tbl2asn.temp.dir"; 

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
	}

	
	public List<Tbl2AsnResult> generateSqnFiles(CommandContext cmdContext, List<String> sourceColumnHeaders, 
			List<Tbl2AsnInput> inputs, byte[] templateBytes, File dataDirFile, boolean sourceInfo) {
		
		String tbl2asnTempDir = getTbl2AsnTempDir(cmdContext);
		String tbl2asnExecutable = getTbl2AsnExecutable(cmdContext);

		String uuid = UUID.randomUUID().toString();
		File tempDir = new File(tbl2asnTempDir, uuid);
		try {
			boolean mkdirsResult = tempDir.mkdirs();
			if((!mkdirsResult) || !(tempDir.exists() && tempDir.isDirectory())) {
				throw new Tbl2AsnException(Code.TBL2ASN_FILE_EXCEPTION, "Failed to create tbl2asn temporary directory: "+tempDir.getAbsolutePath());
			}
			
			File templateFile = new File(tempDir, "template.sbt");
			writeFile(templateFile, templateBytes);
			
			for(Tbl2AsnInput input: inputs) {
				byte[] fastaBytes = FastaUtils.seqIdCompoundsPairToFasta(input.getId(), 
						input.getFastaSequence().getSequenceAsString(), LineFeedStyle.forOS()).getBytes();
				writeFile(new File(tempDir, input.getId()+".fsa"), fastaBytes);
				
				if(sourceInfo) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					OutputStreamCommandResultRenderingContext renderingContext = 
							new OutputStreamCommandResultRenderingContext(baos, ResultOutputFormat.TAB, LineFeedStyle.forOS(), true);
					SourceInfoTableResult sourceInfoTableResult = new SourceInfoTableResult(sourceColumnHeaders, Arrays.asList(input.getSourceInfoMap()));
					sourceInfoTableResult.renderResult(renderingContext);
					writeFile(new File(tempDir, input.getId()+".src"), baos.toByteArray());
				}
				
			}
			
			List<String> commandWords = new ArrayList<String>();
			commandWords.add(tbl2asnExecutable);
			// input directory
			commandWords.add("-p");
			commandWords.add(normalisedFilePath(tempDir));

			/* 
			// batching
			commandWords.add("-a");
			commandWords.add("s");
			*/
			
			// template file
			commandWords.add("-t");
			commandWords.add(normalisedFilePath(templateFile));
			
			ProcessResult tbl2asnProcessResult = ProcessUtils.runProcess(null, tempDir, commandWords); 

			ProcessUtils.checkExitCode(commandWords, tbl2asnProcessResult);

			return resultListFromTempDir(inputs, tempDir);
		} finally {
			ProcessUtils.cleanUpTempDir(dataDirFile, tempDir);
		}
	}
	
	
	private List<Tbl2AsnResult> resultListFromTempDir(List<Tbl2AsnInput> inputs, File tempDir) {
		List<Tbl2AsnResult> results = new ArrayList<Tbl2AsnResult>();
		
		for(Tbl2AsnInput input: inputs) {
			String id = input.getId();
			File sqnFile = new File(tempDir, id+".sqn");
			if(!sqnFile.exists()) {
				throw new Tbl2AsnException(Code.TBL2ASN_FILE_EXCEPTION, "Expected file was not generated: "+id+".sqn");
			}
			byte[] sqnFileContent = readFile(sqnFile);
			results.add(new Tbl2AsnResult(input.getSourceName(), input.getSequenceID(), id, sqnFileContent));
		}
		return results;
	}


	private String getTbl2AsnExecutable(CommandContext cmdContext) {
		String tbl2asnExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(TBL2ASN_EXECUTABLE_PROPERTY);
		if(tbl2asnExecutable == null) { throw new Tbl2AsnException(Code.TBL2ASN_CONFIG_EXCEPTION, "tbl2asn executable not defined in config property "+TBL2ASN_EXECUTABLE_PROPERTY); }
		return tbl2asnExecutable;
	}

	private String getTbl2AsnTempDir(CommandContext cmdContext) {
		String tbl2asnTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(TBL2ASN_TEMP_DIR_PROPERTY);
		if(tbl2asnTempDir == null) { throw new Tbl2AsnException(Code.TBL2ASN_CONFIG_EXCEPTION, "tbl2asn temp directory not defined in config property "+TBL2ASN_TEMP_DIR_PROPERTY); }
		return tbl2asnTempDir;
	}

	
	private String normalisedFilePath(File file) {
		String normalizedPath = file.getAbsolutePath();
		if(System.getProperty("os.name").toLowerCase().contains("windows")) {
			normalizedPath = normalizedPath.replace('\\', '/');
		}
		return normalizedPath;
	}

	
	private void writeFile(File file, byte[] bytes) {
		try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			IOUtils.write(bytes, fileOutputStream);
		} catch (IOException e) {
			throw new Tbl2AsnException(e, Code.TBL2ASN_FILE_EXCEPTION, "Failed to write "+file.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}
	
	private byte[] readFile(File file) {
		try(FileInputStream fileInputStream = new FileInputStream(file)) {
			return IOUtils.toByteArray(fileInputStream);
		} catch (IOException e) {
			throw new Tbl2AsnException(e, Code.TBL2ASN_FILE_EXCEPTION, "Failed to read "+file.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}
	
	private static class SourceInfoTableResult extends BaseTableResult<Map<String, String>> {

		public SourceInfoTableResult(List<String> columnHeaders, List<Map<String, String>> sourceInfoMaps) {
			super("sourceInfoTableResult", sourceInfoMaps, buildColumns(columnHeaders));
		}

		
		@SuppressWarnings("unchecked")
		private static TableColumn<Map<String, String>>[] buildColumns(List<String> columnHeaders) {
			TableColumn<Map<String, String>>[] columns = new TableColumn[columnHeaders.size()];
			for(int i = 0; i < columnHeaders.size(); i++) {
				String header = columnHeaders.get(i);
				columns[i] = column(header, m -> m.get(header));
			}
			return columns;
		}


	}
	
	
}