var intermediateRegions;
glue.inMode("module/exampleTabularUtilityCsv", function() {
	intermediateRegions = glue.command(["load-tabular", "tabular/m49_countries/m49_intermediate_regions.csv"], {convertTableToObjects:true});
});
_.each(intermediateRegions, function(intermediateRegion) {
	var intermediateRegionName = intermediateRegion["Intermediate Region Name"].trim();
	var intermediateRegionId = intermediateRegionName.toLowerCase().replace(/[ -]/g, "_");
	var m49Code = intermediateRegion["Intermediate Region Code"].trim();
	var subRegionName = intermediateRegion["Sub-region Name"].trim();
	var subRegionId = subRegionName.toLowerCase().replace(/[ -]/g, "_");
	glue.command(["create", "custom-table-row", "m49_intermediate_region", intermediateRegionId]);
	glue.inMode("custom-table-row/m49_intermediate_region/"+intermediateRegionId, function() {
		glue.command(["set", "field", "display_name", intermediateRegionName]);
		glue.command(["set", "field", "m49_code", m49Code]);
		glue.command(["set", "link-target", "m49_sub_region", "custom-table-row/m49_sub_region/"+subRegionId]);
	});
});
