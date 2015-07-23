var hcvApp = angular.module('hcvApp', ['angularTreeview']);

hcvApp.controller('searchCtrl', [ '$scope', function($scope) {
		
	$scope.treedata = 
		[
		    { "label" : "All HCV", "id" : "HCV", "children" : [
		        { "label" : "Genotype 1", "id" : "genotype_1", "children" : 
		        	[
				        { "label" : "Subtype 1a", "id" : "genotype_1a", "children" : [] },
				        { "label" : "Subtype 1b", "id" : "genotype_1b", "children" : [] },
				        { "label" : "Subtype 1c", "id" : "genotype_1c", "children" : [] },
		        	 ] },
		        { "label" : "Genotype 2", "id" : "genotype_2", "children" : 
		        	[
				        { "label" : "Subtype 2a", "id" : "genotype_2a", "children" : [] },
				        { "label" : "Subtype 2b", "id" : "genotype_2b", "children" : [] },
		        	 ] },
		    ]},
		];   
	
	
	
	$scope.$watch( 'hcvGenotypes.currentNode', function( newObj, oldObj ) {
	    if( $scope.hcvGenotypes && angular.isObject($scope.hcvGenotypes.currentNode) ) {
	        $scope.genotypeID = $scope.hcvGenotypes.currentNode.id;
	    }
	}, false);

} ]);