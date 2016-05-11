var glueWebToolConfig = angular.module('glueWebToolConfig', []);


glueWebToolConfig.factory('glueWebToolConfig', function () {
	var analysisToolURL;
	var glueWSURL;
	var rendererDialogs;
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
		},
		setRendererDialogs: function(newRendererDialogs) {
			rendererDialogs = newRendererDialogs;
			console.log("rendererDialogs set to: "+rendererDialogs);
		},
		getRendererDialogs: function() {
			return rendererDialogs;
		}
	};
});

