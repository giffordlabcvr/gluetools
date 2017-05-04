function tableResultGetColumn(tableResult, columnHeader) {
	var innerObject = _.pairs(tableResult)[0][1];
	var columnIndex = _.indexOf(innerObject.column, columnHeader);
	var rowsArray = innerObject.row;
	return _.map(rowsArray, function(rowObject) { return rowObject.value[columnIndex];} );
}

function tableResultAsObjectList(tableResult) {
	var innerObject = _.pairs(tableResult)[0][1];
	var columns = innerObject.column;
	var rows = innerObject.row;
	var objectList = [];
	for(var j = 0; j < rows.length; j++) {
		var object = {};
		for(var i = 0; i < columns.length; i++) {
			object[columns[i]] = rows[j].value[i];
		}
		objectList.push(object);
	}
	return objectList;
}

function featureTreeToFeatureList(featureTree) {
	console.log("invoking featureTreeToFeatureList");
	var featureList = [];
	if(featureTree.features != null) { 
		for(var i = 0; i < featureTree.features.length; i++) {
			var feature = {};
			feature.featureName = featureTree.features[i].featureName;
			feature.featureMetatag = featureTree.features[i].featureMetatag;
			feature.featureDescription = featureTree.features[i].featureDescription;
			feature.featureRenderedName = featureTree.features[i].featureRenderedName;
			featureList.push(feature);
		}
		for(var i = 0; i < featureTree.features.length; i++) {
			var childFeatures = featureTreeToFeatureList(featureTree.features[i]);
			for(var j = 0; j < childFeatures.length; j++) {
				featureList.push(childFeatures[j]);
			}
		}
	}
	console.log("featureList", featureList);
	return featureList;
} 

