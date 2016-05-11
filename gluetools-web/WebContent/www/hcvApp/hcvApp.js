var hcvApp = angular.module('hcvApp', [
    'ngRoute',
    'analysisTool', 
    'home',
    'glueWS',
    'glueWebToolConfig'
  ]);

hcvApp.config(['$routeProvider',
  function($routeProvider) {
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
	$scope.analysisToolMenuTitle = "Sequence file analysis";
	glueWS.setProjectURL("../../../gluetools-ws/project/hcv");
	glueWebToolConfig.setAnalysisToolURL("../analysisTool");
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


