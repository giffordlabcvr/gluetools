gluetoolsApp.controller('coreSchemaCtrl', 
		[ '$scope', '$http', '$location', '$anchorScroll', 'dialogs',
			function($scope, $http, $location, $anchorScroll, dialogs) {
			$scope.scrollTo = function(id) {
				var old = $location.hash();
				$location.hash(id);
				$anchorScroll();
				$location.hash(old);
			};

		    
		    $scope.notationDialog = function() {
	    		var dlg = dialogs.create("dialogs/notationDialog.html",
	    				'notationDialogCtrl', {}, {});
		    }
		    
		} ]);
