analysisTool.directive('svgDefs', function(moduleURLs) {
	  return {
		    restrict: 'E',
		    replace: true,
		    scope: {
		    },
		    templateNamespace: 'svg',
		    templateUrl: moduleURLs.getAnalysisToolURL()+'/views/svgDefs.html'
		  };
		});