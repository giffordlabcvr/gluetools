package uk.ac.gla.cvr.gluetools.programs.raxml.epa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.jplace.JPlaceResult;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.NewickPhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlException;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlException.Code;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class RaxmlEpaRunner implements Plugin {

	public static final String SUBSTITUTION_MODEL = "substitutionModel";
	public static final String THOROUGH_INSERTION_FRACTION = "thoroughInsertionFraction";
	public static final String RANDOM_NUMBER_SEED = "randomNumberSeed";
	
	private String substitutionModel = "GTRCAT";
	private Double thoroughInsertionFraction = 0.1;
	private Integer randomNumberSeed = 12345;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		substitutionModel = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SUBSTITUTION_MODEL, false)).orElse(substitutionModel);
		thoroughInsertionFraction = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, THOROUGH_INSERTION_FRACTION, false)).orElse(thoroughInsertionFraction);
		randomNumberSeed = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, RANDOM_NUMBER_SEED, false)).orElse(randomNumberSeed);
	}
	
	public RaxmlEpaResult executeRaxmlEpa(CommandContext cmdContext, PhyloTree phyloTree, Map<String, DNASequence> alignment) {

		String raxmlTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(RaxmlUtils.RAXML_TEMP_DIR_PROPERTY);
		if(raxmlTempDir == null) { throw new RaxmlException(Code.RAXML_CONFIG_EXCEPTION, "RAxML temp directory not defined"); }

		String raxmlExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(RaxmlUtils.RAXMLHPC_EXECUTABLE_PROPERTY);
		if(raxmlExecutable == null) { throw new RaxmlException(Code.RAXML_CONFIG_EXCEPTION, "RAxML executable not defined"); }

		int raxmlCpus = Integer.parseInt(cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(RaxmlUtils.RAXMLHPC_NUMBER_CPUS, "1"));
		
		checkPhyloTree(phyloTree);
		checkAlignment(alignment);
		
		String uuid = UUID.randomUUID().toString();
		File tempDir = new File(raxmlTempDir, uuid);
		try {
			boolean mkdirsResult = tempDir.mkdirs();
			if((!mkdirsResult) || !(tempDir.exists() && tempDir.isDirectory())) {
				throw new RaxmlException(Code.RAXML_FILE_EXCEPTION, "Failed to create RAxML temporary directory: "+tempDir.getAbsolutePath());
			}
			File phyloTreeFile = new File(tempDir, "phyloTree.newick");
			writePhyloTreeFile(tempDir, phyloTreeFile, phyloTree);

			File alignmentFile = new File(tempDir, "alignment.fasta");
			writeAlignmentFile(tempDir, alignmentFile, alignment);
			
			String runSpecifier = "GLUE";
			
			List<String> commandWords = new ArrayList<String>();
			commandWords.add(raxmlExecutable);
			// activate EPA
			commandWords.add("-f");
			commandWords.add("v");
			// threshold insertions
			if(this.thoroughInsertionFraction != null) {
				commandWords.add("-G");
				commandWords.add(Double.toString(thoroughInsertionFraction));
			}
			// alignment file
			commandWords.add("-s");
			commandWords.add(alignmentFile.getAbsolutePath());
			// phylo tree file
			commandWords.add("-t");
			commandWords.add(phyloTreeFile.getAbsolutePath());
			// substitution model
			commandWords.add("-m");
			commandWords.add(this.substitutionModel);
			// random number seed
			commandWords.add("-p");
			commandWords.add(Integer.toString(this.randomNumberSeed));
			// threads / number of CPUs
			commandWords.add("-T");
			commandWords.add(Integer.toString(raxmlCpus));
			// run specifier
			commandWords.add("-n");
			commandWords.add(runSpecifier);
			
			ProcessResult raxmlEpaProcessResult = ProcessUtils.runProcess(null, tempDir, commandWords); 

			if(raxmlEpaProcessResult.getExitCode() != 0) {
				GlueLogger.getGlueLogger().severe("RAxML process "+uuid+" failure, the RAxML stdout was:");
				GlueLogger.getGlueLogger().severe(new String(raxmlEpaProcessResult.getOutputBytes()));
				GlueLogger.getGlueLogger().severe("RAxML process "+uuid+" failure, the RAxML stderr was:");
				GlueLogger.getGlueLogger().severe(new String(raxmlEpaProcessResult.getErrorBytes()));
				throw new RaxmlException(Code.RAXML_PROCESS_EXCEPTION, "RAxML process "+uuid+" failed, see log for output/error content");
			}

			return resultObjectFromTempDir(tempDir, runSpecifier);
		} finally {
			boolean allFilesDeleted = true;
			for(File file : tempDir.listFiles()) {
				boolean fileDeleteResult = file.delete();
				if(!fileDeleteResult) {
					GlueLogger.getGlueLogger().warning("Failed to delete temporary RAxML file "+file.getAbsolutePath());
					allFilesDeleted = false;
					break;
				}
			}
			if(allFilesDeleted) {
				boolean dirDeleteResult = tempDir.delete();
				if(!dirDeleteResult) {
					GlueLogger.getGlueLogger().warning("Failed to delete temporary RAxML directory "+tempDir.getAbsolutePath());
				}
			}
		}
	}

	private RaxmlEpaResult resultObjectFromTempDir(File tempDir,
			String runSpecifier) {
		RaxmlEpaResult raxmlEpaResult = new RaxmlEpaResult();
		File jPlaceFile = new File(tempDir, "RAxML_portableTree."+runSpecifier+".jplace");
		byte[] jPlaceBytes;
		try(FileInputStream fileInputStream = new FileInputStream(jPlaceFile)) {
			jPlaceBytes = IOUtils.toByteArray(fileInputStream);
		} catch (IOException e) {
			throw new RaxmlException(Code.RAXML_FILE_EXCEPTION, "Failed to read RAxML output file: "+jPlaceFile.getAbsolutePath());
		}
		JsonObject jsonObject = JsonUtils.stringToJsonObject(new String(jPlaceBytes));
		JPlaceResult jPlaceResult = JPlaceResult.parse(jsonObject);
		raxmlEpaResult.setjPlaceResult(jPlaceResult);
		return raxmlEpaResult;
	}




	private void writeAlignmentFile(File tempDir, File alignmentFile, Map<String, DNASequence> alignment) {
		byte[] fastaBytes = FastaUtils.mapToFasta(alignment);
		try(FileOutputStream fileOutputStream = new FileOutputStream(alignmentFile)) {
			IOUtils.write(fastaBytes, fileOutputStream);
		} catch (IOException e) {
			throw new RaxmlException(e, Code.RAXML_FILE_EXCEPTION, "Failed to write "+alignmentFile.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}

	private void writePhyloTreeFile(File tempDir, File phyloTreeFile, PhyloTree phyloTree) {
		NewickPhyloTreeVisitor newickPhyloTreeVisitor = new NewickPhyloTreeVisitor();
		phyloTree.accept(newickPhyloTreeVisitor);
		String newickString = newickPhyloTreeVisitor.getNewickString();
		try(FileOutputStream fileOutputStream = new FileOutputStream(phyloTreeFile)) {
			IOUtils.write(newickString.getBytes(), fileOutputStream);
		} catch (IOException e) {
			throw new RaxmlException(e, Code.RAXML_FILE_EXCEPTION, "Failed to write "+phyloTreeFile.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}




	private void checkAlignment(Map<String, DNASequence> alignment) {
		for(String string: alignment.keySet()) {
			if(!RaxmlUtils.validRaxmlName(string)) {
				throw new RaxmlException(Code.RAXML_DATA_EXCEPTION, "Alignment contains row name \""+string+"\" which is invalid in RAxML");
			}
		}
	}




	private void checkPhyloTree(PhyloTree phyloTree) {
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String name = phyloLeaf.getName();
				if(name == null) {
					throw new RaxmlException(Code.RAXML_DATA_EXCEPTION, "Phylo tree contains leaf node without any name");
				}
				if(!RaxmlUtils.validRaxmlName(name)) {
					throw new RaxmlException(Code.RAXML_DATA_EXCEPTION, "Phylo tree contains leaf name \""+name+"\" which is invalid in RAxML");
				}
			}
			@Override
			public void preVisitInternal(PhyloInternal phyloInternal) {
				if(phyloInternal.getBranches().size() != 2) {
					throw new RaxmlException(Code.RAXML_DATA_EXCEPTION, "Phylo tree contains branch which is not strictly bifurcating");
				}
			}
		});
		
	}
	
	
}
