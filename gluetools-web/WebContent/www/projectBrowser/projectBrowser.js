'use strict';

var projectBrowser = angular.module('projectBrowser', 
		['glueWS', 'ui.bootstrap','dialogs.main','ngFileSaver','angularSpinner','glueWebToolConfig']);

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