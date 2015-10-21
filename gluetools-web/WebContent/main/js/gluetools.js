var glueToolsApp = angular.module('gluetoolsApp', [
  'ngRoute',
  'home',
  'download', 
  'installation',
  'model'
]);

glueToolsApp.config(['$routeProvider',
    function($routeProvider) {
      $routeProvider.
    	when('/download', {
            templateUrl: 'gluetools/download.html',
            controller: 'downloadCtrl'
          }).
      	when('/installation', {
            templateUrl: 'gluetools/installation.html',
            controller: 'installationCtrl'
          }).
      	when('/model', {
            templateUrl: 'gluetools/model.html',
            controller: 'modelCtrl'
          }).
        when('/home', {
            templateUrl: 'gluetools/home.html',
            controller: 'homeCtrl'
          }).
        otherwise({
          redirectTo: '/home'
        });
    }]);

glueToolsApp.controller('gluetoolsCtrl', 
[ '$scope',
  function ($scope) {
  	$scope.brand = "GLUEtools";
  	$scope.homeMenuTitle = "Home";
  	$scope.downloadMenuTitle = "Download";
  	$scope.documentationMenuTitle = "Documentation";
  	$scope.modelMenuTitle = "Model";
  	$scope.installationMenuTitle = "Installation";
  } ]);

