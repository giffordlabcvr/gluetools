analysisTool.directive('svgDefs', function(glueWebToolConfig) {
	  return {
		    restrict: 'E',
		    replace: true,
		    scope: {
		    },
		    templateNamespace: 'svg',
		    templateUrl: glueWebToolConfig.getAnalysisToolURL()+'/views/svgDefs.html'
		  };
		});