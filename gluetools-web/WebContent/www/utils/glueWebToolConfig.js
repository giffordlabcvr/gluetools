var glueWebToolConfig = angular.module('glueWebToolConfig', []);


glueWebToolConfig.factory('glueWebToolConfig', function () {
	var analysisToolURL;
	var projectBrowserURL;
	var glueWSURL;
	var rendererDialogs;
	var analysisModuleName;
	return {
		setAnalysisToolURL: function(newURL) {
			analysisToolURL = newURL;
			console.log("analysis tool URL set to: "+newURL);
		},
		getAnalysisToolURL: function() {
			return analysisToolURL;
		},
		setProjectBrowserURL: function(newURL) {
			projectBrowserURL = newURL;
			console.log("project browser URL set to: "+newURL);
		},
		getProjectBrowserURL: function() {
			return projectBrowserURL;
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
		},
		setAnalysisModuleName: function(newAnalysisModuleName) {
			analysisModuleName = newAnalysisModuleName;
			console.log("analysisModuleName set to: "+analysisModuleName);
		},
		getAnalysisModuleName: function() {
			return analysisModuleName;
		}
	};
});

