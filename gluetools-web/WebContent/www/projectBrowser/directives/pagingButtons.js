projectBrowser.directive('pagingButtons', function(glueWebToolConfig) {
	  return {
		    restrict: 'E',
		    controller: function($scope) {
		    },
		    replace: true,
		    scope: {
		      pagingContext: '='
		    },
		    templateUrl: glueWebToolConfig.getProjectBrowserURL()+'/views/pagingButtons.html'
		  };
		});