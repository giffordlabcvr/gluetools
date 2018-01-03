function nullTrim(string) {
	if(string == null) {
		return null;
	}
	return string.trim();
}

var countries;
var displayNameObjs;
glue.inMode("module/exampleTabularUtilityTab", function() {
	countries = glue.command(["load-tabular", "tabular/m49_countries/m49_countries.txt"], {convertTableToObjects:true});
	displayNameObjs = glue.command(["load-tabular", "tabular/m49_countries/m49_country_display_names.txt"], {convertTableToObjects:true});
});
var idToDisplayName = {};
_.each(displayNameObjs, function(displayNameObj) {
	idToDisplayName[displayNameObj["id"]] = displayNameObj["Display Name"];
});
_.each(countries, function(country) {
	var countryId = nullTrim(country["ISO-alpha3 Code"]);
	var fullName = nullTrim(country["Country or Area"]);
	var displayName;
	if(countryId != null) {
		displayName = idToDisplayName[countryId];	
	}
	var m49Code = nullTrim(country["M49 Code"]);
	var regionName = nullTrim(country["Region Name"]);
	var regionId;
	if(regionName != null) {
		regionId = regionName.toLowerCase();
	}
	var subRegionName = nullTrim(country["Sub-region Name"]);
	var subRegionId;
	if(subRegionName != null) {
		subRegionId = subRegionName.toLowerCase().replace(/[ -]/g, "_");
	}
	var intermediateRegionName = nullTrim(country["Intermediate Region Name"]);
	var intermediateRegionId; 
	if(intermediateRegionName != null) {
		intermediateRegionId = intermediateRegionName.toLowerCase().replace(/[ -]/g, "_");	
	}
	var ldc = nullTrim(country["Least Developed Countries (LDC)"]);
	var lldc = nullTrim(country["Land Locked Developing Countries (LLDC)"]);
	var sids = nullTrim(country["Small Island Developing States (SIDS)"]);
	var developmentStatus = nullTrim(country["Developed / Developing Countries"]);
	if(countryId != null) {
		glue.command(["create", "custom-table-row", "m49_country", countryId]);
		glue.inMode("custom-table-row/m49_country/"+countryId, function() {
			glue.command(["set", "field", "full_name", fullName]);
			if(displayName != null) {
				glue.command(["set", "field", "display_name", displayName]);
			}
			if(m49Code != null) {
				glue.command(["set", "field", "m49_code", m49Code]);
			}
			if(regionId != null) {
				glue.command(["set", "link-target", "m49_region", "custom-table-row/m49_region/"+regionId]);
			}
			if(subRegionId != null) {
				glue.command(["set", "link-target", "m49_sub_region", "custom-table-row/m49_sub_region/"+subRegionId]);
			}
			if(intermediateRegionId != null) {
				glue.command(["set", "link-target", "m49_intermediate_region", "custom-table-row/m49_intermediate_region/"+intermediateRegionId]);
			}
			glue.command(["set", "field", "is_ldc", ldc == 'x']);
			glue.command(["set", "field", "is_lldc", lldc == 'x']);
			glue.command(["set", "field", "is_sids", sids == 'x']);
			if(developmentStatus != null) {	
				glue.command(["set", "field", "development_status", developmentStatus]);
			}
		});
	}
});
