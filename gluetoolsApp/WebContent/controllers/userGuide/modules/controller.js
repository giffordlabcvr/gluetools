gluetoolsApp.controller('modulesCtrl', 
		[ '$scope', '$http', '$location', '$anchorScroll',
			function($scope, $http, $location, $anchorScroll) {

			$scope.scrollTo = function(id) {
				var old = $location.hash();
				$location.hash(id);
				$anchorScroll();
				$location.hash(old);
			};

		    $http.get('./controllers/userGuide/modules/raxmlPhylogenyGenerator.xml')
	        .success(function(data) {
	            $scope.raxmlPhylogenyGenerator = data;
	            console.log("raxmlPhylogenyGenerator", $scope.raxmlPhylogenyGenerator);
	        });

		} ]);
