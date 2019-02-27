gluetoolsApp.controller('deepSequencingDataCtrl', 
		[ '$scope', '$http', '$location', '$anchorScroll',
			function($scope, $http, $location, $anchorScroll) {

			$scope.scrollTo = function(id) {
				var old = $location.hash();
				$location.hash(id);
				$anchorScroll();
				$location.hash(old);
			};

		    $http.get('./exampleProject/modules/exampleSamReporter.xml')
	        .success(function(data) {
	            $scope.exampleSamReporterConfig = data;
	            console.log("exampleSamReporter", $scope.exampleSamReporterConfig);
	        });

		    $http.get('./controllers/userGuide/deepSequencingData/sam-config.xml')
	        .success(function(data) {
	            $scope.samConfig = data;
	            console.log("samConfig", $scope.samConfig);
	        });

		} ]);
