
// Given a (coding) genome feature and start and end codon positions
// For each example sequence, list its sequence ID, host species
// and the amino acid translation and underlying nucleotides for that region
function hostWithGenomeRegion(feature, startCodon, endCodon) {
    // where clause used at various points to select the example sequence AlignmentMembers
    var whereClause = "sequence.source.name = 'ncbi-hev-examples'";
    
    // object used as an associative map from sequenceID to result row
    var resultRowMap = {};
    
    glue.inMode("alignment/AL_MASTER", function() {
        // list the alignment members, retrieving sequence ID and host species
        var listMemberResults = glue.tableToObjects(glue.command(["list", "member", "--recursive", "--whereClause", whereClause, 
                      "sequence.sequenceID", "sequence.host_species"]));
        // for each alignment member, create a result row object, with sequenceID and hostSpecies fields
        // and add it to the result row map
        _.each(listMemberResults, function(listMemberResult) {
            var sequenceID = listMemberResult["sequence.sequenceID"];
            var memberObj = { 
                sequenceID: sequenceID, 
                hostSpecies: listMemberResult["sequence.host_species"], 
            }
            resultRowMap[sequenceID] = memberObj;
        });
    });

    // run the protein alignment exporter to generate the specified genome region for each example sequence
    var aaRegionColumnHeader = "aminoAcids_"+feature+"_"+startCodon+"_to_"+endCodon;
    glue.inMode("module/exampleEcmaProteinAlignmentExporter", function() {
        var aaGenomeRegions = glue.command(["export", "AL_MASTER", "--relRefName", "REF_MASTER_M73218", 
                                            "--featureName", feature, "--labelledCodon", startCodon, endCodon, 
                                            "--recursive", "--whereClause", whereClause, "--preview"]);
        // add each region to the appropriate result row
        _.each(aaGenomeRegions.aminoAcidFasta.sequences, function(region) {
            resultRowMap[region.id][aaRegionColumnHeader] = region.sequence;
        });
    });

    // run the nucleotide alignment exporter to generate the specified genome region for each example sequence
    var ntRegionColumnHeader = "nucleotides_"+feature+"_"+startCodon+"_to_"+endCodon;
    glue.inMode("module/exampleEcmaNtAlignmentExporter", function() {
        var ntGenomeRegions = glue.command(["export", "AL_MASTER", "--relRefName", "REF_MASTER_M73218", 
                                            "--featureName", feature, "--labelledCodon", startCodon, endCodon, 
                                            "--recursive", "--whereClause", whereClause, "--preview"]);
        // add each region to the appropriate result row
        _.each(ntGenomeRegions.nucleotideFasta.sequences, function(region) {
            resultRowMap[region.id][ntRegionColumnHeader] = region.sequence;
        });
    });
    
    // return result row objects as a list.
    return _.values(resultRowMap);
}