gluetoolsApp.controller('aboutCtrl', 
		[ '$scope', '$http', '$location', '$anchorScroll',
			function($scope, $http, $location, $anchorScroll) {

			$scope.scrollTo = function(id) {
				var old = $location.hash();
				$location.hash(id);
				$anchorScroll();
				$location.hash(old);
			};

		} ]);
