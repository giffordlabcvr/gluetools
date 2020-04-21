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
package uk.ac.gla.cvr.gluetools.programs.raxml.epa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.jplace.JPlaceResult;
import uk.ac.gla.cvr.gluetools.core.modules.PropertyGroup;
import uk.ac.gla.cvr.gluetools.core.newick.NewickGenerator;
import uk.ac.gla.cvr.gluetools.core.newick.NewickJPlaceToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickGenerator;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
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
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

public class RaxmlEpaRunner extends RaxmlRunner {

	// phylo leaf user data key to use for leaf name when exporting tree to EPA
	public static final String EPA_LEAF_NAME_USER_DATA_KEY = "epaLeafName";
	
	public static final String THOROUGH_INSERTION_FRACTION = "thoroughInsertionFraction";
	
	public static final String KEEP_PLACEMENTS = "keepPlacements";
	public static final String PROB_THRESHOLD = "probThreshold";
	public static final String ACCUMULATED_THRESHOLD = "accumulatedThreshold";
	
	private Double thoroughInsertionFraction = 0.1;
	private Integer keepPlacements = null;
	private Double probThreshold = null;
	private Double accumulatedThreshold = null;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		thoroughInsertionFraction = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, THOROUGH_INSERTION_FRACTION, false)).orElse(thoroughInsertionFraction);
		keepPlacements = PluginUtils.configureIntProperty(configElem, KEEP_PLACEMENTS, 1, true, null, false, false);
		probThreshold = PluginUtils.configureDoubleProperty(configElem, PROB_THRESHOLD, 0.0, false, 1.0, true, false);
		accumulatedThreshold = PluginUtils.configureDoubleProperty(configElem, ACCUMULATED_THRESHOLD, 0.0, false, 1.0, true, false);
		if(accumulatedThreshold != null && (keepPlacements != null || probThreshold != null)) {
			throw new RaxmlException(Code.RAXML_CONFIG_EXCEPTION, "If raxmlEpaRunner accumulatedThreshold is used, neither keepPlacements nor probThreshold may be used.");
		}
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
		
		SubstitutionModel substitutionModel = this.getSubstitutionModel();
		if(!substitutionModel.isNucleotide()) {
			throw new RaxmlException(Code.RAXML_CONFIG_EXCEPTION, "RAxML EPA cannot run on non-nucleotide substitution model: "+substitutionModel.name());
		}
		
		checkPhyloTree(phyloTree);
		checkAlignment(alignment);
		
		// figure out the query rows by deleting each leaf name from the tree.
		Set<String> queryRows = new LinkedHashSet<String>(alignment.keySet());
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String taxonName = (String) phyloLeaf.ensureUserData().get(EPA_LEAF_NAME_USER_DATA_KEY);
				queryRows.remove(taxonName);
			}
		});
		if(queryRows.isEmpty()) {
			// no query rows. 
			// Rather than running raxml epa and getting an error (it doesn't like zero inputs), 
			// form up an empty JPlaceResult.
			// The only snag here is make sure the contained tree has integer branch labels.
			JPlaceResult jplaceResult = new JPlaceResult();
			// slightly hacky way to clone the tree.
			String newick = phyloTreeToRaxmlInputNewick(phyloTree);
			PhyloTree jplacePhyloTree = PhyloFormat.NEWICK.parse(newick.getBytes());
			jplacePhyloTree.accept(new PhyloTreeVisitor() {
				int edgeIndex = 0;
				@Override
				public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
					phyloBranch.ensureUserData().put(NewickJPlaceToPhyloTreeParser.J_PLACE_BRANCH_LABEL, new Integer(edgeIndex));
					edgeIndex++;
				}
			});
			jplaceResult.setTree(jplacePhyloTree);
			jplaceResult.setFields(Arrays.asList("edge_num", "likelihood", "like_weight_ratio", "distal_length", "pendant_length"));
			RaxmlEpaResult raxmlEpaResult = new RaxmlEpaResult();
			raxmlEpaResult.setjPlaceResult(jplaceResult);
			return raxmlEpaResult;
		}
		
		byte[] alignmentFastaBytes = FastaUtils.mapToFasta(alignment, LineFeedStyle.LF);

		
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
			writeAlignmentFile(tempDir, alignmentFile, alignmentFastaBytes);
			
			String runSpecifier = "GLUE";
			
			List<String> commandWords = new ArrayList<String>();
			commandWords.add(raxmlExecutable);
			// activate EPA
			commandWords.add("-f");
			commandWords.add("v");
			
			// various EPA settings
			if(this.keepPlacements != null) {
				commandWords.add("--epa-keep-placements="+Integer.toString(this.keepPlacements));
			}
			if(this.probThreshold != null) {
				commandWords.add("--epa-prob-threshold="+Double.toString(this.probThreshold));
			}
			if(this.accumulatedThreshold != null) {
				commandWords.add("--epa-accumulated-threshold="+Double.toString(this.accumulatedThreshold));
			}
			
			// threshold insertions
			if(this.thoroughInsertionFraction != null) {
				commandWords.add("-G");
				commandWords.add(Double.toString(thoroughInsertionFraction));
			}
			// alignment file
			commandWords.add("-s");
			
			commandWords.add(normalisedFilePath(alignmentFile));
			// phylo tree file
			commandWords.add("-t");
			commandWords.add(normalisedFilePath(phyloTreeFile));
			// substitution model
			commandWords.add("-m");
			commandWords.add(substitutionModel.name());
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

			if(raxmlEpaProcessResult.getExitCode() == 134) {
				String errorString = new String(raxmlEpaProcessResult.getErrorBytes());
				if(errorString.contains("setRateModel: Assertion `rate >= 0.0001 && rate <= 1000000.0' failed")) {
					throw new RaxmlEpaException(RaxmlEpaException.Code.RAXML_EPA_EXIT_138_ASSERTION_ERROR, 
							"RAxML EPA exited with code 138 and the stderr mentioned a failed setRateModel assertion. This is a known bug which can be worked around by changing the set of inputs.");
				}
			}

			
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
		String newickString = phyloTreeToRaxmlInputNewick(phyloTree);
		try(FileOutputStream fileOutputStream = new FileOutputStream(phyloTreeFile)) {
			IOUtils.write(newickString.getBytes(), fileOutputStream);
		} catch (IOException e) {
			throw new RaxmlException(e, Code.RAXML_FILE_EXCEPTION, "Failed to write "+phyloTreeFile.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}

	private String phyloTreeToRaxmlInputNewick(PhyloTree phyloTree) {
		PhyloTreeToNewickGenerator newickPhyloTreeVisitor = new PhyloTreeToNewickGenerator(new NewickGenerator() {
			@Override
			public String generateLeafName(PhyloLeaf phyloLeaf) {
				return (String) phyloLeaf.ensureUserData().get(EPA_LEAF_NAME_USER_DATA_KEY);
			}
			
		});
		phyloTree.accept(newickPhyloTreeVisitor);
		String newickString = newickPhyloTreeVisitor.getNewickString();
		return newickString;
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
