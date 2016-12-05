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
import uk.ac.gla.cvr.gluetools.core.modules.PropertyGroup;
import uk.ac.gla.cvr.gluetools.core.newick.NewickGenerator;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickGenerator;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlException;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlException.Code;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlRunner;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class RaxmlEpaRunner extends RaxmlRunner {

	// phylo leaf user data key to use for leaf name when exporting tree to EPA
	public static final String EPA_LEAF_NAME_USER_DATA_KEY = "epaLeafName";
	
	public static final String THOROUGH_INSERTION_FRACTION = "thoroughInsertionFraction";
	
	private Double thoroughInsertionFraction = 0.1;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		thoroughInsertionFraction = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, THOROUGH_INSERTION_FRACTION, false)).orElse(thoroughInsertionFraction);
	}
	
	@Override
	public void configurePropertyGroup(PropertyGroup propertyGroup) {
		super.configurePropertyGroup(propertyGroup);
		propertyGroup
			.addPropertyName(THOROUGH_INSERTION_FRACTION);
	}

	
	public RaxmlEpaResult executeRaxmlEpa(CommandContext cmdContext, PhyloTree phyloTree, Map<String, DNASequence> alignment, File dataDirFile) {

		String raxmlTempDir = getRaxmlTempDir(cmdContext);
		String raxmlExecutable = getRaxmlExecutable(cmdContext);
		int raxmlCpus = getRaxmlCpus(cmdContext);
		
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
			commandWords.add(this.getSubstitutionModel());
			// random number seed
			commandWords.add("-p");
			commandWords.add(Integer.toString(this.getRandomNumberSeed1()));
			// threads / number of CPUs
			commandWords.add("-T");
			commandWords.add(Integer.toString(raxmlCpus));
			// run specifier
			commandWords.add("-n");
			commandWords.add(runSpecifier);
			
			ProcessResult raxmlEpaProcessResult = ProcessUtils.runProcess(null, tempDir, commandWords); 

			ProcessUtils.checkExitCode(commandWords, raxmlEpaProcessResult);

			return resultObjectFromTempDir(tempDir, runSpecifier);
		} finally {
			ProcessUtils.cleanUpTempDir(dataDirFile, tempDir);
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
		String jPlaceString = new String(jPlaceBytes);
		JsonObject jsonObject = JsonUtils.stringToJsonObject(jPlaceString);
		JPlaceResult jPlaceResult = JPlaceResult.parse(jsonObject);
		raxmlEpaResult.setjPlaceResult(jPlaceResult);
		return raxmlEpaResult;
	}




	private void writePhyloTreeFile(File tempDir, File phyloTreeFile, PhyloTree phyloTree) {
		PhyloTreeToNewickGenerator newickPhyloTreeVisitor = new PhyloTreeToNewickGenerator(new NewickGenerator() {
			@Override
			public String generateLeafName(PhyloLeaf phyloLeaf) {
				return (String) phyloLeaf.ensureUserData().get(EPA_LEAF_NAME_USER_DATA_KEY);
			}
			
		});
		phyloTree.accept(newickPhyloTreeVisitor);
		String newickString = newickPhyloTreeVisitor.getNewickString();
		try(FileOutputStream fileOutputStream = new FileOutputStream(phyloTreeFile)) {
			IOUtils.write(newickString.getBytes(), fileOutputStream);
		} catch (IOException e) {
			throw new RaxmlException(e, Code.RAXML_FILE_EXCEPTION, "Failed to write "+phyloTreeFile.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}




	private void checkPhyloTree(PhyloTree phyloTree) {
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				Object value = phyloLeaf.ensureUserData().get(EPA_LEAF_NAME_USER_DATA_KEY);
				if(value == null) {
					throw new RaxmlException(Code.RAXML_DATA_EXCEPTION, "Phylo tree leaf node has no String value for user data key "+EPA_LEAF_NAME_USER_DATA_KEY);
				}
				if(!(value instanceof String)) {
					throw new RaxmlException(Code.RAXML_DATA_EXCEPTION, "Phylo tree leaf node value for user data key "+EPA_LEAF_NAME_USER_DATA_KEY+" is not a String");
				}
				String epaLeafName = (String) value;
				if(!RaxmlUtils.validRaxmlName(epaLeafName)) {
					throw new RaxmlException(Code.RAXML_DATA_EXCEPTION, "Phylo tree contains leaf with EPA name \""+epaLeafName+"\" which is invalid in RAxML");
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
