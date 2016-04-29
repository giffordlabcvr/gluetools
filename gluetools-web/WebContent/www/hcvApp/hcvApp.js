var hcvApp = angular.module('hcvApp', [
    'ngRoute',
    'submitSequencesAnalysis', 
    'home',
    'glueWS',
    'moduleURLs'
  ]);

hcvApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/submitSequencesAnalysis', {
          templateUrl: 'submitSequencesAnalysis.html',
          controller: 'submitSequencesAnalysisCtrl'
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
  [ '$scope', 'glueWS', 'moduleURLs',
function ($scope, glueWS, moduleURLs) {
	$scope.brand = "HCV-GLUE";
	$scope.homeMenuTitle = "Home";
	$scope.analysisMenuTitle = "Analysis";
	$scope.submitSequencesAnalysisMenuTitle = "Submit sequences for analysis";
	glueWS.setProjectURL("../../../gluetools-ws/project/hcv");
	moduleURLs.setAnalysisToolURL("../analysisTool");
	moduleURLs.setGlueWSURL("../glueWS");
} ]);


