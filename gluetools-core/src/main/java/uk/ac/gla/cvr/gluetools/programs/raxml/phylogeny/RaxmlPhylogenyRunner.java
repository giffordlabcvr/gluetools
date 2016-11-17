package uk.ac.gla.cvr.gluetools.programs.raxml.phylogeny;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.modules.PropertyGroup;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.NewickToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlException;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlException.Code;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlRunner;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class RaxmlPhylogenyRunner extends RaxmlRunner {

	public static final String BOOTSTRAP_REPLICATES = "bootstrapReplicates";
	public static final String RANDOM_NUMBER_SEED_2 = "randomNumberSeed2";
	
	private Integer bootstrapReplicates = 100;
	private Integer randomNumberSeed2 = 45678;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.bootstrapReplicates = Optional.ofNullable(
				PluginUtils.configureIntProperty(configElem, BOOTSTRAP_REPLICATES, false)).orElse(bootstrapReplicates);
		this.randomNumberSeed2 = 
				Optional
				.ofNullable(PluginUtils.configureIntProperty(configElem, RANDOM_NUMBER_SEED_2, false))
				.orElse(randomNumberSeed2);
	}

	@Override
	public void configurePropertyGroup(PropertyGroup propertyGroup) {
		super.configurePropertyGroup(propertyGroup);
		propertyGroup
			.addPropertyName(BOOTSTRAP_REPLICATES)
			.addPropertyName(RANDOM_NUMBER_SEED_2);
	}

	
	public RaxmlPhylogenyResult executeRaxmlPhylogeny(CommandContext cmdContext, Map<String, DNASequence> alignment, File dataDirFile) {

		String raxmlTempDir = getRaxmlTempDir(cmdContext);
		String raxmlExecutable = getRaxmlExecutable(cmdContext);
		int raxmlCpus = getRaxmlCpus(cmdContext);
		
		checkAlignment(alignment);
		
		String uuid = UUID.randomUUID().toString();
		File tempDir = new File(raxmlTempDir, uuid);
		try {
			boolean mkdirsResult = tempDir.mkdirs();
			if((!mkdirsResult) || !(tempDir.exists() && tempDir.isDirectory())) {
				throw new RaxmlException(Code.RAXML_FILE_EXCEPTION, "Failed to create RAxML temporary directory: "+tempDir.getAbsolutePath());
			}
			File alignmentFile = new File(tempDir, "alignment.fasta");
			writeAlignmentFile(tempDir, alignmentFile, alignment);
			
			String runSpecifier = "GLUE";
			
			List<String> commandWords = new ArrayList<String>();
			commandWords.add(raxmlExecutable);
			// ML tree search with bootstrapping
			commandWords.add("-f");
			commandWords.add("a");
			// random number seed 2
			commandWords.add("-x");
			commandWords.add(Integer.toString(randomNumberSeed2));
			// alignment file
			commandWords.add("-s");
			commandWords.add(alignmentFile.getAbsolutePath());
			// substitution model
			commandWords.add("-m");
			commandWords.add(this.getSubstitutionModel());
			// random number seed 1
			commandWords.add("-p");
			commandWords.add(Integer.toString(this.getRandomNumberSeed1()));
			// threads / number of CPUs
			commandWords.add("-T");
			commandWords.add(Integer.toString(raxmlCpus));
			// number of bootstraps
			commandWords.add("-N");
			commandWords.add(Integer.toString(bootstrapReplicates));
			// run specifier
			commandWords.add("-n");
			commandWords.add(runSpecifier);
			
			ProcessResult raxmlProcessResult = ProcessUtils.runProcess(null, tempDir, commandWords); 

			ProcessUtils.checkExitCode(commandWords, raxmlProcessResult);

			return resultObjectFromTempDir(tempDir, runSpecifier);
		} finally {
			ProcessUtils.cleanUpTempDir(dataDirFile, tempDir);
		}
	}

	private RaxmlPhylogenyResult resultObjectFromTempDir(File tempDir, String runSpecifier) {
		RaxmlPhylogenyResult result = new RaxmlPhylogenyResult();
		// the best tree, with support percentages as node labels.
		File bestTreeFile = new File(tempDir, "RAxML_bipartitions."+runSpecifier);
		byte[] bestTreeBytes;
		try(FileInputStream fileInputStream = new FileInputStream(bestTreeFile)) {
			bestTreeBytes = IOUtils.toByteArray(fileInputStream);
		} catch (IOException e) {
			throw new RaxmlException(Code.RAXML_FILE_EXCEPTION, "Failed to read RAxML output file: "+bestTreeFile.getAbsolutePath());
		}
		String bestTreeString = new String(bestTreeBytes);
		NewickToPhyloTreeParser newickToPhyloTreeParser = new NewickToPhyloTreeParser();
		PhyloTree phyloTree = newickToPhyloTreeParser.parseNewick(bestTreeString);
		// could add info log, bootstrap trees etc. in here I guess.
		result.setPhyloTree(phyloTree);
		return result;
	}

	
}
