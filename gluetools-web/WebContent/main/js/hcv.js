var hcvApp = angular.module('hcvApp', ['angularTreeview', 'ui.bootstrap']);

hcvApp.factory('GenotypeSelection', function (){
	return {
		id: "",
		label: "",
		sequences: [], 
		showSequences: false
	};
});

hcvApp.controller('selectGenotypeCtrl', [ '$scope', '$http', 'GenotypeSelection',
function($scope, $http, GenotypeSelection) {
	$scope.GenotypeSelection = GenotypeSelection;

	$scope.defaultOpenDepth = 1;
	$scope.defaultSelectedId = "HCV";
	
	$scope.genotypes = null
    $http.get('../main/js/hcvGenotypes.json')
        .success(function(data) {
            $scope.genotypes = [ data ];
        })
        .error(function(data,status,error,config){
            $scope.contents = [{heading:"Error",description:"Could not load json data"}];
        });
	
	$scope.$watch( 'genotypesTree.currentNode', function( newObj, oldObj ) {
	    if( $scope.GenotypeSelection && $scope.genotypesTree && angular.isObject($scope.genotypesTree.currentNode) ) {
	        var node = $scope.genotypesTree.currentNode;
	    	$scope.GenotypeSelection.id = node.id;
	        $scope.GenotypeSelection.label = node.label;
	        var sequences = node.sequences || [];
	        $scope.GenotypeSelection.sequences = _.map(sequences, function(seq) {
	        	return {accession:seq, url:"http://www.ncbi.nlm.nih.gov/nuccore/"+seq};
	        }); 
	        $scope.GenotypeSelection.showSequences = sequences.length > 0;
	    }
	}, false);

} ]);


hcvApp.controller('accordionCtrl', [ '$scope', 'GenotypeSelection', 
function ($scope, GenotypeSelection) {
	$scope.GenotypeSelection = GenotypeSelection;
	$scope.genotypeSelectHeading = "Select genotype";  
	$scope.genotypeSelectOpen = true;
} ]);
