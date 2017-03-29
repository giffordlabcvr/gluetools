projectBrowser.directive('pagingButtons', function(glueWebToolConfig) {
	  return {
		    restrict: 'E',
		    controller: function($scope) {
		    },
		    transclude: true,
		    replace: true,
		    scope: {
		      pagingContext: '='
		    },
		    templateUrl: glueWebToolConfig.getProjectBrowserURL()+'/views/pagingButtons.html'
		  };
		});