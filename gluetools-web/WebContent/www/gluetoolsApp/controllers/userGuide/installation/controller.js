gluetoolsApp.controller('installationCtrl', 
		[ '$scope', '$http', '$location', '$anchorScroll',
		function($scope, $http, $location, $anchorScroll) {
		    $http.get('./downloads/gluetools-config.xml')
	        .success(function(data) {
	            $scope.gluetoolsXml = data;
	        });

		    $scope.scrollTo = function(id) {
		        $location.hash(id);
		        $anchorScroll();
		     }
		    
		    
		} ]);
