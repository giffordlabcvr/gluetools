gluetoolsApp.controller('coreSchemaCtrl', 
		[ '$scope', '$http', '$location', '$anchorScroll', 'dialogs',
			function($scope, $http, $location, $anchorScroll, dialogs) {
		    $scope.scrollTo = function(id) {
		        $location.hash(id);
		        $anchorScroll();
		     }

		    
		    $scope.notationDialog = function() {
	    		var dlg = dialogs.create("dialogs/notationDialog.html",
	    				'notationDialogCtrl', {}, {});
		    }
		    
		} ]);
