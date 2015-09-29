var hcvApp = angular.module('gluetoolsApp', [
  'ngRoute',
  'home',
  'download'
]);

hcvApp.config(['$routeProvider',
    function($routeProvider) {
      $routeProvider.
      	when('/download', {
          templateUrl: 'gluetools/download.html',
          controller: 'downloadCtrl'
        }).
        when('/home', {
            templateUrl: 'gluetools/home.html',
            controller: 'homeCtrl'
          }).
        otherwise({
          redirectTo: '/home'
        });
    }]);

hcvApp.controller('gluetoolsCtrl', 
[ '$scope',
  function ($scope) {
  	$scope.brand = "GLUEtools";
  	$scope.homeMenuTitle = "Home";
  	$scope.downloadMenuTitle = "Download";
  } ]);

