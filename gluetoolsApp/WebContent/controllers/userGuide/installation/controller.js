gluetoolsApp.controller('installationCtrl', 
		[ '$scope', '$http', '$location', '$anchorScroll',
		function($scope, $http, $location, $anchorScroll) {
		    $http.get('./downloads/gluetools-config.xml')
	        .success(function(data) {
	            $scope.gluetoolsXml = data;
	        });

		    $http.get('./controllers/userGuide/installation/basic-config.xml')
	        .success(function(data) {
	            $scope.basicConfig = data;
	            console.log("basicConfig", $scope.basicConfig);
	        });

		    $http.get('./controllers/userGuide/installation/cygwin-config.xml')
	        .success(function(data) {
	            $scope.cygwinConfig = data;
	            console.log("cygwinConfig", $scope.cygwinConfig);
	        });

		    $http.get('./controllers/userGuide/installation/blast-config.xml')
	        .success(function(data) {
	            $scope.blastConfig = data;
	        });

		    $http.get('./controllers/userGuide/installation/raxml-config.xml')
	        .success(function(data) {
	            $scope.raxmlConfig = data;
	        });

		    $http.get('./controllers/userGuide/installation/mafft-config.xml')
	        .success(function(data) {
	            $scope.mafftConfig = data;
	        });

			$scope.scrollTo = function(id) {
				var old = $location.hash();
				$location.hash(id);
				$anchorScroll();
				$location.hash(old);
			};
		    
		    
		} ]);
