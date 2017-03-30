projectBrowser.directive('genomeFeatureTree', function(glueWebToolConfig) {
	  return {
		    restrict: 'E',
		    controller: function($scope) {
		    	$scope.treeOptions = {
		    		    nodeChildren: "features",
		    		    dirSelectable: true,
		    		    allowDeselect: false,
		    		    injectClasses: {
		    		        ul: "a1",
		    		        li: "a2",
		    		        liSelected: "a7",
		    		        iExpanded: "a3",
		    		        iCollapsed: "a4",
		    		        iLeaf: "a5",
		    		        label: "a6",
		    		        labelSelected: "a8"
		    		    }
		    		}
		    		
	    		$scope.expandedNodes = []; 
	    		
	    		$scope.selectedNode = null;
	    		
	    		$scope.expandAll = function(node) {
	    			$scope.expandedNodes.push(node);
	    			_.each(node.features, function(n) {$scope.expandAll(n)} );
	    		}
	    		$scope.$watch('featureTree', function() {
	    			if($scope.featureTree != null) {
	    				$scope.expandAll($scope.featureTree);
	    				if($scope.featureTree.features.length > 0) {
	    					$scope.selectedNode = $scope.featureTree.features[0];
	    				}
	    			}
	    	    });
		    },
		    replace: true,
		    scope: {
			      featureTree: '=',
			      selectedNode: '='
		    },
		    templateUrl: glueWebToolConfig.getProjectBrowserURL()+'/views/genomeFeatureTree.html'
		  };
		});