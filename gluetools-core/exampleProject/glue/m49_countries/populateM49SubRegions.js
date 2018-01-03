var subRegions;
glue.inMode("module/exampleTabularUtilityCsv", function() {
	subRegions = glue.command(["load-tabular", "tabular/m49_countries/m49_sub_regions.csv"], {convertTableToObjects:true});
});
_.each(subRegions, function(subRegion) {
	var subRegionName = subRegion["Sub-region Name"].trim();
	var subRegionId = subRegionName.toLowerCase().replace(/[ -]/g, "_");
	var m49Code = subRegion["Sub-region Code"].trim();
	var regionName = subRegion["Region Name"].trim();
	var regionId = regionName.toLowerCase();
	glue.command(["create", "custom-table-row", "m49_sub_region", subRegionId]);
	glue.inMode("custom-table-row/m49_sub_region/"+subRegionId, function() {
		glue.command(["set", "field", "display_name", subRegionName]);
		glue.command(["set", "field", "m49_code", m49Code]);
		glue.command(["set", "link-target", "m49_region", "custom-table-row/m49_region/"+regionId]);
	});
});
