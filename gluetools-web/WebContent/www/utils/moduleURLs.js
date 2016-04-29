var moduleURLs = angular.module('moduleURLs', []);


moduleURLs.factory('moduleURLs', function () {
	var analysisToolURL;
	var glueWSURL;
	return {
		setAnalysisToolURL: function(newURL) {
			analysisToolURL = newURL;
			console.log("analysis tool URL set to: "+newURL);
		},
		getAnalysisToolURL: function() {
			return analysisToolURL;
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

