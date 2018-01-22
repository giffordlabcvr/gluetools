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
