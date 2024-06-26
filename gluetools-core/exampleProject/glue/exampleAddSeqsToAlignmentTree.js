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

function addToAlignment(sourceName, sequenceID, almtName) {
	glue.inMode("alignment/"+almtName, function() {
		glue.command(["add", "member", sourceName, sequenceID]);
	});
	glue.command(["compute", "alignment", almtName, "exampleCompoundAligner", 
	              "--whereClause", "sequence.source.name = '"+sourceName+"' and sequence.sequenceID = '"+sequenceID+"'"]);
}

var genotypingResults;

glue.inMode("module/exampleMaxLikelihoodGenotyper", function() {
	genotypingResults = glue.tableToObjects(glue.command([
	 "genotype", "sequence", "--whereClause", "source.name in ('ncbi-hev-examples', 'fasta-hev-examples')"]));
});

for(var i = 0; i < genotypingResults.length; i++) {
	var result = genotypingResults[i];
	var queryNameBits = result.queryName.split("/");
	var sourceName = queryNameBits[0];
	var sequenceID = queryNameBits[1];
	
	var genotypeAlmt = result.genotypeFinalClade;
	var subtypeAlmt = result.subtypeFinalClade;
	if(genotypeAlmt != null) {
		
		if(subtypeAlmt != null) {
			addToAlignment(sourceName, sequenceID, subtypeAlmt);
		} else {
			addToAlignment(sourceName, sequenceID, genotypeAlmt);
		}
	}

	glue.inMode("sequence/"+sourceName+"/"+sequenceID, function() {

		if(genotypeAlmt != null) {
			var gtRegex = /AL_([\d])/;
			var gtMatch = gtRegex.exec(genotypeAlmt);
			if(gtMatch) {
				glue.command(["set", "field", "genotype", gtMatch[1]]);
			}
			if(subtypeAlmt != null) {
				var stRegex = /AL_[\d](.+)/;
				var stMatch = stRegex.exec(subtypeAlmt);
				if(stMatch) {
					glue.command(["set", "field", "subtype", stMatch[1]]);
				}
			}
		}
	});
	
}
