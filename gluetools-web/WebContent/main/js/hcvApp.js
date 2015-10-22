var hcvApp = angular.module('hcvApp', [
  'ngRoute',
  'datasetAnalysis', 
  'submitSequencesAnalysis', 
  'home',
  'glueWS'
]);

hcvApp.config(['$routeProvider',
    function($routeProvider) {
      $routeProvider.
      	when('/datasetAnalysis', {
          templateUrl: 'hcvApp/datasetAnalysis.html',
          controller: 'datasetAnalysisCtrl'
        }).
        when('/submitSequencesAnalysis', {
            templateUrl: 'hcvApp/submitSequencesAnalysis.html',
            controller: 'submitSequencesAnalysisCtrl'
          }).
        when('/home', {
            templateUrl: 'hcvApp/home.html',
            controller: 'homeCtrl'
          }).
        otherwise({
          redirectTo: '/home'
        });
    }]);

hcvApp.controller('hcvAppCtrl', 
[ '$scope',
  function ($scope) {
  	$scope.brand = "HCV-GLUE";
  	$scope.homeMenuTitle = "Home";
  	$scope.projectMenuTitle = "Project";
  	$scope.projectSourcesMenuTitle = "Sources";
  	$scope.projectSequencesMenuTitle = "Sequences";
  	$scope.analysisMenuTitle = "Analysis";
  	$scope.submitSequencesAnalysisMenuTitle = "Submit sequences for analysis";
  } ]);

