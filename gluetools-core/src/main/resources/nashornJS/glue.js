var valueToNative = function(inValue) {
	if(inValue instanceof Java.type("java.util.List")) {
		return listToNative(inValue);
	} else if(inValue instanceof Java.type("java.util.Map")) {
		return objToNative(inValue);
	} else {
		return inValue;
	}
};

var objToNative = function(inObj) {
	var native = {};
	for (key in inObj) {
		native[key] = valueToNative(inObj[key]);
	}
	return native;
};

var listToNative = function(inList) {
	var native = [];
	for (var i = 0; i < inList.length; i++) {
		native.push(valueToNative(inList[i]));
	}
	return native;
};

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

function recastException(x) {
	var recastEx = new Error(x.getMessage());
	recastEx.javaEx = x;
	return(recastEx);
}

// the point of this layer is to always throw an ECMAScript exception
// which means we will get the JS file, line number and column number context.
var glue = {
		log: function(level, message, object) {
			var logMessage = message;
			if(object) {
				logMessage = logMessage+"\n"+JSON.stringify(object, null, 2); 
			}
			try {
				glueAux.log(level, logMessage);
			} catch(x) {
				throw recastException(x);
			}
		},
		command: function(cmdDocument) {
			try {
				return valueToNative(glueAux.command(cmdDocument));
			} catch(x) {
				throw recastException(x);
			}
		},
		pushMode: function(modePath) {
			try {
				glueAux.pushMode(modePath);
			} catch(x) {
				throw recastException(x);
			}
		},
		popMode: function() {
			try {
				glueAux.popMode();
			} catch(x) {
				throw recastException(x);
			}
		},
		currentMode: function() {
			try {
				return glueAux.currentMode();
			} catch(x) {
				throw recastException(x);
			}
		}
};

