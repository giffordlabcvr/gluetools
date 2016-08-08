console.log("before hcvApp module definition");

var hcvApp = angular.module('hcvApp', [
    'ngRoute',
    'analysisTool', 
    'projectBrowser', 
    'home',
    'glueWS',
    'glueWebToolConfig'
  ]);

console.log("after hcvApp module definition");

hcvApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/analysisTool', {
        templateUrl: '../analysisTool/analysisTool.html',
        controller: 'analysisToolCtrl'
      }).
      // the below routing should maybe happen inside the projectBrowser module?
      when('/project/reference', {
    	  templateUrl: '../projectBrowser/views/references.html',
    	  controller: 'referencesCtrl'
      }).
      when('/project/reference/:refName', {
    	  templateUrl: '../projectBrowser/views/reference.html',
    	  controller: 'referenceCtrl'
      }).
      when('/project/sequence', {
    	  templateUrl: '../projectBrowser/views/sequences.html',
    	  controller: 'sequencesCtrl'
      }).
      when('/project/sequence/:sourceName/:sequenceID', {
    	  templateUrl: '../projectBrowser/views/sequence.html',
    	  controller: 'sequenceCtrl'
      }).
      when('/project/alignment', {
    	  templateUrl: '../projectBrowser/views/alignments.html',
    	  controller: 'alignmentsCtrl'
      }).
      when('/project/alignment/:alignmentName', {
    	  templateUrl: '../projectBrowser/views/alignment.html',
    	  controller: 'alignmentCtrl'
      }).
      when('/project/alignment/:alignmentName/member/:sourceName/:sequenceID', {
    	  templateUrl: '../projectBrowser/views/alignmentMember.html',
    	  controller: 'alignmentMemberCtrl'
      }).
      when('/home', {
    	  templateUrl: './modules/home/home.html',
    	  controller: 'homeCtrl'
      }).
      otherwise({
    	  redirectTo: '/home'
      });
}]);

hcvApp.controller('hcvAppCtrl', 
  [ '$scope', 'glueWS', 'glueWebToolConfig',
function ($scope, glueWS, glueWebToolConfig) {
	$scope.brand = "HCV-GLUE";
	$scope.homeMenuTitle = "Home";
	$scope.analysisMenuTitle = "Analysis";
	$scope.analysisToolMenuTitle = "Sequence typing and interpretation";
	$scope.projectBrowserMenuTitle = "Project";
	$scope.projectBrowserAlignmentMenuTitle = "Clades";
	$scope.projectBrowserReferenceSequenceMenuTitle = "Reference sequences";
	$scope.projectBrowserSequenceMenuTitle = "Sequences";
	glueWS.setProjectURL("../../../gluetools-ws/project/hcv");
	glueWebToolConfig.setAnalysisToolURL("../analysisTool");
	glueWebToolConfig.setProjectBrowserURL("../projectBrowser");
	glueWebToolConfig.setGlueWSURL("../glueWS");
	glueWebToolConfig.setRendererDialogs([
	                              	    {
	                            	    	renderer: "hcvEpitopeRenderer",
	                            	    	dialogURL: "dialogs/displayEpitope.html",
	                            	    	dialogController: "displayEpitopeCtrl"
	                            	    },
	                            	    {
	                            	    	renderer: "hcvCommonAaPolymorphismRenderer",
	                            	    	dialogURL: "dialogs/displayCommonAa.html",
	                            	    	dialogController: "displayCommonAaCtrl"
	                            	    },
	                            	    {
	                            	    	renderer: "hcvResistanceAssociatedVariantRenderer",
	                            	    	dialogURL: "dialogs/displayRAV.html",
	                            	    	dialogController: "displayRAVCtrl"
	                            	    }
	]);
} ]);


