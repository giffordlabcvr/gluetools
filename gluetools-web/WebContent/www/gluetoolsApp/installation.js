var installation = angular.module('installation', ['ui.bootstrap', 'hljs']);


installation.controller('installationCtrl', 
		[ '$scope', '$http', 
		function($scope, $http) {
		    $http.get('gluetools/downloads/gluetools-config.xml')
	        .success(function(data) {
	            $scope.gluetoolsXml = data;
	        });
			
		} ]);
