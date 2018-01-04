var regions;
glue.inMode("module/exampleTabularUtilityCsv", function() {
	regions = glue.command(["load-tabular", "tabular/m49_countries/m49_regions.csv"], {convertTableToObjects:true});
});
_.each(regions, function(region) {
	var regionName = region["Region Name"].trim();
	var regionId = regionName.toLowerCase();
	var m49Code = region["Region Code"].trim();
	glue.command(["create", "custom-table-row", "m49_region", regionId]);
	glue.inMode("custom-table-row/m49_region/"+regionId, function() {
		glue.command(["set", "field", "display_name", regionName]);
		glue.command(["set", "field", "m49_code", m49Code]);
	});
});
