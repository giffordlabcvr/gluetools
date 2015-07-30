var hcvApp = angular.module('hcvApp', [
  'ngRoute',
  'mutationsBrowser', 
  'home'
]);

hcvApp.config(['$routeProvider',
    function($routeProvider) {
      $routeProvider.
        when('/mutationsBrowser', {
          templateUrl: 'mutationsBrowser.html',
          controller: 'mutationsBrowserCtrl'
        }).
        when('/home', {
            templateUrl: 'home.html',
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
  } ]);

