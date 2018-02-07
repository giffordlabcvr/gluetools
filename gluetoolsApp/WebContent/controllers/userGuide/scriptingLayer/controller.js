gluetoolsApp.controller('scriptingLayerCtrl', 
		[ '$scope', '$http', '$location', '$anchorScroll',
			function($scope, $http, $location, $anchorScroll) {

			$scope.scrollTo = function(id) {
				var old = $location.hash();
				$location.hash(id);
				$anchorScroll();
				$location.hash(old);
			};

		    $http.get('./exampleProject/modules/exampleEcmaFunctionInvoker.js')
	        .success(function(data) {
	        	$scope.exampleEcmaFunctionInvokerJavaScript = data;
	            console.log("$scope.exampleEcmaFunctionInvokerJavaScript", $scope.exampleEcmaFunctionInvokerJavaScript);
	        });

		    $http.get('./exampleProject/modules/exampleEcmaFunctionInvoker.xml')
	        .success(function(data) {
	        	$scope.exampleEcmaFunctionInvokerXml = data;
	            console.log("$scope.exampleEcmaFunctionInvokerXml", $scope.exampleEcmaFunctionInvokerXml);
	        });

		} ]);
