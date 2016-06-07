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

