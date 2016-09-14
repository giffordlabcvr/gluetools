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

hcvApp.config(['$routeProvider', 'projectBrowserStandardRoutesProvider',
  function($routeProvider, projectBrowserStandardRoutesProvider) {
	
	var projectBrowserStandardRoutes = projectBrowserStandardRoutesProvider.$get();
	var projectBrowserURL = "../projectBrowser";

	projectBrowserStandardRoutes.addReferencesRoute($routeProvider, projectBrowserURL);
	projectBrowserStandardRoutes.addReferenceRoute($routeProvider, projectBrowserURL);
	projectBrowserStandardRoutes.addSequencesRoute($routeProvider, projectBrowserURL);
	projectBrowserStandardRoutes.addSequenceRoute($routeProvider, projectBrowserURL);
	projectBrowserStandardRoutes.addAlignmentsRoute($routeProvider, projectBrowserURL);
	projectBrowserStandardRoutes.addAlignmentRoute($routeProvider, projectBrowserURL);
	projectBrowserStandardRoutes.addAlignmentMemberRoute($routeProvider, projectBrowserURL);
	
    $routeProvider.
      when('/analysisTool', {
        templateUrl: '../analysisTool/analysisTool.html',
        controller: 'analysisToolCtrl'
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


