console.log("loading utils");

function toFixed(value, precision) {
    var precision = precision || 0,
        power = Math.pow(10, precision),
        absValue = Math.abs(Math.round(value * power)),
        result = (value < 0 ? '-' : '') + String(Math.floor(absValue / power));

    if (precision > 0) {
        var fraction = String(absValue % power),
            padding = new Array(Math.max(precision - fraction.length, 0) + 1).join('0');
        result += '.' + padding + fraction;
    }
    return result;
}

function addUtilsToScope($scope) {
	$scope.toFixed = function(v,p) { return toFixed(v,p); }
	$scope.renderDisplayName = renderDisplayName;
	$scope.handleNull = handleNull;
	$scope.collectionYearRange = collectionYearRange;
	$scope.b64ToBlob = b64ToBlob;
	$scope.spacerString = spacerString;
	$scope.truncate = truncate;
}

function spacerString(numSpaces) {
	var string = "";
	for(var i = 0; i < numSpaces; i++) {
		string = string+"\u00A0"
	}
	return string;
}

function truncate(string, maxChars) {
	if(string.length <= maxChars || maxChars < 5) {
		return string;
	}
	return(string.substring(0, maxChars-3) + "...");
}

function renderDisplayName(name, displayName) {
	if(displayName == null) {
		return name;
	}
	return displayName;
}

function handleNull(text) {
	if(text == null) {
		return "-";
	}
	return text;
}; 

function collectionYearRange(earliest, latest) {
	if(earliest == null && latest == null) {
		return "-";
	}
	if(earliest != null && latest != null) {
		if(earliest == latest) {
			return earliest;
		} else {
			return earliest + " to " + latest;
		}
		
	}
	if(earliest == null) {
		return latest + " or earlier";
	}
	return earliest + " or later";
}; 


// find the index of the first item X in list for which predicate(X) returns true, 
// or return -1 if there is no such item.
// it's assumed that if predicate is true for any member of the list, then it 
// is true for all subsequent members

// https://en.wikipedia.org/wiki/Binary_search_algorithm
function binarySearch(list, predicate) {
	var L = 0;
	var R = list.length - 1;
	var m;
	while(L < R) {
		m = Math.floor((L + R) / 2);
		// console.log("L:"+L+", m:"+m+", R:"+R);
		if(predicate(list[m])) {
			R = m;
		} else {
			L = m+1;
		}
	}
	if(L == R) {
		if(predicate(list[L])) {
			return L;
		} else {
			return -1;
		}
	}
	if(L > R) {
		return -1;
	}
}


function b64ToBlob(b64Data, contentType, sliceSize) {
	  contentType = contentType || '';
	  sliceSize = sliceSize || 512;

	  var byteCharacters = atob(b64Data);
	  var byteArrays = [];

	  for (var offset = 0; offset < byteCharacters.length; offset += sliceSize) {
	    var slice = byteCharacters.slice(offset, offset + sliceSize);

	    var byteNumbers = new Array(slice.length);
	    for (var i = 0; i < slice.length; i++) {
	      byteNumbers[i] = slice.charCodeAt(i);
	    }

	    var byteArray = new Uint8Array(byteNumbers);

	    byteArrays.push(byteArray);
	  }

	  var blob = new Blob(byteArrays, {type: contentType});
	  return blob;
}
