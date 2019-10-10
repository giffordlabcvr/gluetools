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
package uk.ac.gla.cvr.gluetools.core.clusterPickerRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.clusterPickerRunner.ClusterPickerException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.tabularUtility.TabularUtility;
import uk.ac.gla.cvr.gluetools.core.tabularUtility.TabularUtility.TabularData;
import uk.ac.gla.cvr.gluetools.programs.java.JavaChildProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@PluginClass(elemName="clusterPickerRunner",
	description="Binding to software for identifying supported clusters within a tree")
public class ClusterPickerRunner extends ModulePlugin<ClusterPickerRunner> {

	private static final String INPUT_TREE_FILENAME = "input-tree";
	public static String 
		CLUSTER_PICKER_JAR_PATH_PROPERTY = "gluetools.core.programs.clusterPicker.jarPath"; 
	public static String 
		CLUSTER_PICKER_TEMP_DIR_PROPERTY = "gluetools.core.programs.clusterPicker.temp.dir"; 

	public static final String INITIAL_THRESHOLD = "initialThreshold";
	public static final String SUPPORT_THRESHOLD = "supportThreshold";
	public static final String GENETIC_THRESHOLD = "geneticThreshold";
	public static final String LARGE_CLUSTER_THRESHOLD = "largeClusterThreshold";
	public static final String DIFF_TYPE = "diffType";
	public static final String PHYLO_FIELD_NAME = "phyloFieldName";
	public static final String NODE_THRESHOLD_TYPE = "nodeThresholdType";
	
	
	private String phyloFieldName;
	private double initialThreshold;
	private double supportThreshold;
	private double geneticThreshold;
	private int largeClusterThreshold;
	private DiffType diffType;
	private NodeThresholdType nodeThresholdType;
	
	public enum DiffType {
		gap,
		abs,
		valid,
		ambiguity
	}
	
	public enum NodeThresholdType {
		BOOTSTRAPS, 
		TRANSFER_BOOTSTRAPS
	}


	
	public ClusterPickerRunner() {
		super();
		registerModulePluginCmdClass(RunClusterPickerFieldCommand.class);
		registerModulePluginCmdClass(RunClusterPickerFileCommand.class);
		addSimplePropertyName(INITIAL_THRESHOLD);
		addSimplePropertyName(SUPPORT_THRESHOLD);
		addSimplePropertyName(GENETIC_THRESHOLD);
		addSimplePropertyName(LARGE_CLUSTER_THRESHOLD);
		addSimplePropertyName(DIFF_TYPE);
		addSimplePropertyName(PHYLO_FIELD_NAME);
		addSimplePropertyName(NODE_THRESHOLD_TYPE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.phyloFieldName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, PHYLO_FIELD_NAME, false)).orElse("phylogeny");
		// note that defaults of 95.0 and 75.0 for bootstrap thresholds match the fact
		// that RAxML outputs trees annotated with bootstraps out of 100.
		this.initialThreshold = Optional.ofNullable(PluginUtils
				.configureDoubleProperty(configElem, INITIAL_THRESHOLD, false)).orElse(95.0);
		this.supportThreshold = Optional.ofNullable(PluginUtils
				.configureDoubleProperty(configElem, SUPPORT_THRESHOLD, false)).orElse(75.0);
		this.geneticThreshold = Optional.ofNullable(PluginUtils
				.configureDoubleProperty(configElem, GENETIC_THRESHOLD, 0.0, true, 1.0, true, false)).orElse(0.045);
		this.largeClusterThreshold = Optional.ofNullable(PluginUtils
				.configureIntProperty(configElem, LARGE_CLUSTER_THRESHOLD, false)).orElse(10);
		this.diffType = Optional
				.ofNullable(PluginUtils
				.configureEnumProperty(DiffType.class, configElem, DIFF_TYPE, false)).orElse(DiffType.gap);
		this.nodeThresholdType = Optional
				.ofNullable(PluginUtils
				.configureEnumProperty(NodeThresholdType.class, configElem, NODE_THRESHOLD_TYPE, false)).orElse(NodeThresholdType.BOOTSTRAPS);

	}

	private String getClusterPickerJarPath(CommandContext cmdContext) {
		String clusterPickerJarPath = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(CLUSTER_PICKER_JAR_PATH_PROPERTY);
		if(clusterPickerJarPath == null) { throw new ClusterPickerException(Code.CLUSTER_PICKER_CONFIG_EXCEPTION, "ClusterPicker jar path not defined in config property "+CLUSTER_PICKER_JAR_PATH_PROPERTY); }
		return clusterPickerJarPath;
	}

	private String getClusterPickerTempDir(CommandContext cmdContext) {
		String clusterPickerTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(CLUSTER_PICKER_TEMP_DIR_PROPERTY);
		if(clusterPickerTempDir == null) { throw new ClusterPickerException(Code.CLUSTER_PICKER_CONFIG_EXCEPTION, "ClusterPicker temp directory not defined in config property "+CLUSTER_PICKER_TEMP_DIR_PROPERTY); }
		return clusterPickerTempDir;
	}
	
	public List<ClusterPickerResultLine> runClusterPicker(CommandContext cmdContext, Map<String, DNASequence> alignment, 
			PhyloTree tree, File dataDirFile) {
		String clusterPickerJarPath = getClusterPickerJarPath(cmdContext);
		String clusterPickerTempDir = getClusterPickerTempDir(cmdContext);
		
		String uuid = UUID.randomUUID().toString();
		File tempDir = new File(clusterPickerTempDir, uuid);
		try {
			boolean mkdirsResult = tempDir.mkdirs();
			if((!mkdirsResult) || !(tempDir.exists() && tempDir.isDirectory())) {
				throw new ClusterPickerException(Code.CLUSTER_PICKER_FILE_EXCEPTION, "Failed to create ClusterPicker temporary directory: "+tempDir.getAbsolutePath());
			}
			
			File alignmentFile = new File(tempDir, "input-fasta.fas");
			byte[] alignmentBytes = FastaUtils.mapToFasta(alignment, LineFeedStyle.forOS());
			writeFile(alignmentFile, alignmentBytes);

			File treeFile = new File(tempDir, INPUT_TREE_FILENAME+".nwk");
			PhyloFormat clusterPickerInputTreeFormat;
			switch(this.nodeThresholdType) {
			case BOOTSTRAPS:
				clusterPickerInputTreeFormat = PhyloFormat.NEWICK_BOOTSTRAPS;
				break;
			case TRANSFER_BOOTSTRAPS:
				clusterPickerInputTreeFormat = PhyloFormat.NEWICK_TRANSFER_BOOTSTRAPS;
				break;
			default:
				throw new ClusterPickerException(Code.CLUSTER_PICKER_CONFIG_EXCEPTION, "Unknown node threshold type: "+this.nodeThresholdType.name());
			}
			byte[] treeBytes = clusterPickerInputTreeFormat.generate(tree);
			writeFile(treeFile, treeBytes);
			
			List<String> commandWords = new ArrayList<String>();
			commandWords.add(JavaChildProcessUtils.getJavaExecutablePath());

			// ClusterPicker jar
			commandWords.add("-jar");
			commandWords.add(ProcessUtils.normalisedFilePath(new File(clusterPickerJarPath)));

			// input alignment
			commandWords.add(ProcessUtils.normalisedFilePath(alignmentFile));
			// input tree
			commandWords.add(ProcessUtils.normalisedFilePath(treeFile));

			// parameter settings
			commandWords.add(Double.toString(this.initialThreshold));
			commandWords.add(Double.toString(this.supportThreshold));
			commandWords.add(Double.toString(this.geneticThreshold));
			commandWords.add(Double.toString(this.largeClusterThreshold));
			commandWords.add(this.diffType.name());

			ProcessResult clusterPickerProcessResult = ProcessUtils.runProcess(null, tempDir, commandWords); 

			ProcessUtils.checkExitCode(commandWords, clusterPickerProcessResult);

			return resultListFromTempDir(tempDir);
		} finally {
			ProcessUtils.cleanUpTempDir(dataDirFile, tempDir);
		}

		
	}
	
	public String getPhyloFieldName() {
		return phyloFieldName;
	}

	private List<ClusterPickerResultLine> resultListFromTempDir(File tempDir) {
		File clusterPicksListFile = new File(tempDir, INPUT_TREE_FILENAME+"_clusterPicks_list.txt");
		byte[] clusterPicksListBytes = readFile(clusterPicksListFile);
		TabularData clusterPicks = TabularUtility.tabularDataFromBytes(clusterPicksListBytes, Pattern.compile("\\t"));
		
		List<ClusterPickerResultLine> results = new ArrayList<ClusterPickerResultLine>();
		List<String[]> rows = clusterPicks.getRows();
		rows.forEach(cpRow -> {
			Map<String,String> memberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, cpRow[0]);
			Integer clusterIndex = Integer.parseInt(cpRow[1]);
			if(clusterIndex == -1) {
				clusterIndex = null; // null cluster assignment: sequence is on its own, not in any cluster.
			}
			results.add(new ClusterPickerResultLine(memberPkMap, clusterIndex));
		});
		results.sort(new Comparator<ClusterPickerResultLine>() {
			@Override
			public int compare(ClusterPickerResultLine o1, ClusterPickerResultLine o2) {
				Integer ci1 = o1.getClusterIndex();
				Integer ci2 = o2.getClusterIndex();
				if(ci1 == null && ci2 == null) {
					return 0;
				}
				if(ci1 != null && ci2 == null) {
					return 1;
				}
				if(ci1 == null && ci2 != null) {
					return -1;
				}
				return Integer.compare(ci1, ci2);
			}
		});
		
		return results;
	}

	private void writeFile(File file, byte[] bytes) {
		try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			IOUtils.write(bytes, fileOutputStream);
		} catch (IOException e) {
			throw new ClusterPickerException(e, Code.CLUSTER_PICKER_FILE_EXCEPTION, "Failed to write "+file.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}
	
	private byte[] readFile(File file) {
		try(FileInputStream fileInputStream = new FileInputStream(file)) {
			return IOUtils.toByteArray(fileInputStream);
		} catch (IOException e) {
			throw new ClusterPickerException(e, Code.CLUSTER_PICKER_FILE_EXCEPTION, "Failed to read "+file.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}
}
