'use strict';

var userAgent = detect.parse(navigator.userAgent);

console.log("userAgent.browser.family", userAgent.browser.family);
console.log("userAgent.browser.name", userAgent.browser.name);
console.log("userAgent.browser.version", userAgent.browser.version);

var projectBrowser = angular.module('projectBrowser', 
		['angularFileUpload', 'glueWS', 'ui.bootstrap','dialogs.main','ngFileSaver','angularSpinner','glueWebToolConfig',
		    'angulartics',
		    'angulartics.google.analytics']);



projectBrowser.factory("projectBrowserStandardRoutes", function() {
	return {
		// could provide standard addXXXRoute functions, e.g.:
		/*
		addAlignmentRoute: function(routeProvider, projectBrowserURL) {
			routeProvider.when('/project/alignment/:alignmentName', {
		    	  templateUrl: projectBrowserURL+'/views/alignment.html',
		    	  controller: 'alignmentCtrl'
		      });
		}, */
	}
});