var glueWebToolConfig = angular.module('glueWebToolConfig', []);


glueWebToolConfig.factory('glueWebToolConfig', function () {
	var analysisToolURL;
	var analysisToolExampleSequenceURL;
	var analysisToolExampleSequenceMsWindowsURL;
	var projectBrowserURL;
	var glueWSURL;
	var analysisModuleName;
	return {
		setAnalysisToolExampleSequenceURL: function(newExampleSequenceURL) {
			analysisToolExampleSequenceURL = newExampleSequenceURL;
			console.log("analysis tool ExampleSequence URL set to: "+newExampleSequenceURL);
		},
		getAnalysisToolExampleSequenceURL: function() {
			return analysisToolExampleSequenceURL;
		},
		setAnalysisToolExampleMsWindowsSequenceURL: function(newExampleMsWindowsSequenceURL) {
			analysisToolExampleMsWindowsSequenceURL = newExampleMsWindowsSequenceURL;
			console.log("analysis tool ExampleMsWindowsSequence URL set to: "+newExampleMsWindowsSequenceURL);
		},
		getAnalysisToolExampleMsWindowsSequenceURL: function() {
			return analysisToolExampleMsWindowsSequenceURL;
		},
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
		setAnalysisModuleName: function(newAnalysisModuleName) {
			analysisModuleName = newAnalysisModuleName;
			console.log("analysisModuleName set to: "+analysisModuleName);
		},
		getAnalysisModuleName: function() {
			return analysisModuleName;
		},
	};
});

