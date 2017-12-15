

function addToAlignment(sourceName, sequenceID, almtName) {
	glue.inMode("alignment/"+almtName, function() {
		glue.command(["add", "member", sourceName, sequenceID]);
	});
	glue.command(["compute", "alignment", almtName, "exampleCompoundAligner", 
	              "--whereClause", "sequence.source.name = '"+sourceName+"' and sequence.sequenceID = '"+sequenceID+"'"]);
}

var genotypingResults;

glue.inMode("module/exampleMaxLikelihoodGenotyper", function() {
	genotypingResults = glue.command(["genotype", "sequence", "--whereClause", "source.name = 'ncbi-hev-examples'"], {convertTableToObjects:true});
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
	
}
