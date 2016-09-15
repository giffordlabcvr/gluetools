'use strict';

var projectBrowser = angular.module('projectBrowser', 
		['glueWS', 'ui.bootstrap','dialogs.main']);

projectBrowser.factory("projectBrowserStandardRoutes", function() {
	return {
		addReferencesRoute: function(routeProvider, projectBrowserURL) {
			routeProvider.when('/project/reference', {
				templateUrl: projectBrowserURL+'/views/references.html',
				controller: 'referencesCtrl'
			});
		},
		addReferenceRoute: function(routeProvider, projectBrowserURL) {
			routeProvider.when('/project/reference/:refName', {
		    	  templateUrl: projectBrowserURL+'/views/reference.html',
		    	  controller: 'referenceCtrl'
		      });
		},
		addSequencesRoute: function(routeProvider, projectBrowserURL) {
			routeProvider.when('/project/sequence', {
		    	  templateUrl: projectBrowserURL+'/views/sequences.html',
		    	  controller: 'sequencesCtrl'
		      });
		},
		addSequenceRoute: function(routeProvider, projectBrowserURL) {
			routeProvider.when('/project/sequence/:sourceName/:sequenceID', {
		    	  templateUrl: projectBrowserURL+'/views/sequence.html',
		    	  controller: 'sequenceCtrl'
		      });
		},
		addAlignmentsRoute: function(routeProvider, projectBrowserURL) {
			routeProvider.when('/project/alignment', {
		    	  templateUrl: projectBrowserURL+'/views/alignments.html',
		    	  controller: 'alignmentsCtrl'
		      });
		},
		addAlignmentRoute: function(routeProvider, projectBrowserURL) {
			routeProvider.when('/project/alignment/:alignmentName', {
		    	  templateUrl: projectBrowserURL+'/views/alignment.html',
		    	  controller: 'alignmentCtrl'
		      });
		},
		addAlignmentMemberRoute: function(routeProvider, projectBrowserURL) {
			routeProvider.when('/project/alignment/:alignmentName/member/:sourceName/:sequenceID', {
		    	  templateUrl: projectBrowserURL+'/views/alignmentMember.html',
		    	  controller: 'alignmentMemberCtrl'
		      });
		}
	}
});