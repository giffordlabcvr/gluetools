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
		command: function(cmdInput, options) {
			if(options === undefined) {
				options = {};
			}
			try {
				var cmdResult;
				if(_.isString(cmdInput)) {
					cmdResult = glueAux.runCommandFromString(cmdInput);
				} else if(_.isArray(cmdInput)) {
					cmdResult = glueAux.runCommandFromArray(cmdInput)
				} else if(_.isObject(cmdInput)){
					cmdResult = glueAux.runCommandFromObject(cmdInput);
				} else {
					throw new Error("Unable to invoke command using input "+cmdInput);
				}
				// this step ensures the result consists of native JavaScript objects;
				cmdResult = valueToNative(cmdResult);
				if(options.convertTableToObjects) {
					cmdResult = tableResultAsObjectList(cmdResult);
				}
				return cmdResult;
			} catch(x) {
				throw recastException(x);
			}
		},
		inMode: function(modePath, inModeFunction) {
			var oldModePath = this.currentMode();
			try {
				this.pushMode(modePath);
				inModeFunction();
			} finally {
				while(this.currentMode() != oldModePath && this.currentMode() != "/") {
					this.popMode();
				}
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

