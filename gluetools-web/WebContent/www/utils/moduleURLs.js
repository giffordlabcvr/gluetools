var moduleURLs = angular.module('moduleURLs', []);


moduleURLs.factory('moduleURLs', function () {
	var analysisAppURL;
	var glueWSURL;
	return {
		setAnalysisAppURL: function(newURL) {
			analysisAppURL = newURL;
			console.log("analysis app URL set to: "+newURL);
		},
		getAnalysisAppURL: function() {
			return analysisAppURL;
		},
		setGlueWSURL: function(newURL) {
			glueWSURL = newURL;
			console.log("glueWS URL set to: "+newURL);
		},
		getGlueWSURL: function() {
			return glueWSURL;
		}
	};
});

