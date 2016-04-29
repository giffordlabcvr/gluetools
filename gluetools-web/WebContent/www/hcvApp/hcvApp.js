var hcvApp = angular.module('hcvApp', [
    'ngRoute',
    'analysisTool', 
    'home',
    'glueWS',
    'moduleURLs'
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
  [ '$scope', 'glueWS', 'moduleURLs',
function ($scope, glueWS, moduleURLs) {
	$scope.brand = "HCV-GLUE";
	$scope.homeMenuTitle = "Home";
	$scope.analysisMenuTitle = "Analysis";
	$scope.analysisToolMenuTitle = "Sequence file analysis";
	glueWS.setProjectURL("../../../gluetools-ws/project/hcv");
	moduleURLs.setAnalysisToolURL("../analysisTool");
	moduleURLs.setGlueWSURL("../glueWS");
} ]);


