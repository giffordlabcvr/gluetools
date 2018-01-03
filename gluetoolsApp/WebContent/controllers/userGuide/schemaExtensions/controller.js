gluetoolsApp.controller('schemaExtensionsCtrl', 
		[ '$scope', '$http', '$location', '$anchorScroll',
			function($scope, $http, $location, $anchorScroll) {

		    $scope.scrollTo = function(id) {
		        $location.hash(id);
		        $anchorScroll();
		     }

		} ]);
