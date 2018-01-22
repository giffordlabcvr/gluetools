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
