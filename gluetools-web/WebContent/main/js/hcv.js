var hcvApp = angular.module('hcvApp', ['angularTreeview', 'ui.bootstrap']);

hcvApp.factory('GenotypeSelection', function (){
	return {
		id: "",
		label: "",
		sequences: [], 
		showSequences: false
	};
});

hcvApp.factory('RegionSelection', function (){
	return {
		id: "",
		label: "", 
		description: ""
	};
});


hcvApp.controller('selectGenotypeCtrl', 
		[ '$scope', '$http', 'GenotypeSelection',
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


hcvApp.controller('selectRegionCtrl', 
		[ '$scope', '$http', 'RegionSelection',
		function($scope, $http, RegionSelection) {
			$scope.RegionSelection = RegionSelection;

			$scope.defaultOpenDepth = 3;
			$scope.defaultSelectedId = "whole_genome";
			
			$scope.regions = null
		    $http.get('../main/js/subgenomicRegions.json')
		        .success(function(data) {
		            $scope.regions = [ data ];
		        })
		        .error(function(data,status,error,config){
		            $scope.contents = [{heading:"Error",description:"Could not load json data"}];
		        });
			
			$scope.$watch( 'regionsTree.currentNode', function( newObj, oldObj ) {
			    if( $scope.RegionSelection && $scope.regionsTree && angular.isObject($scope.regionsTree.currentNode) ) {
			        var node = $scope.regionsTree.currentNode;
			    	$scope.RegionSelection.id = node.id;
			        $scope.RegionSelection.label = node.label;
			        $scope.RegionSelection.description = node.description || "";
			        
			    }
			}, false);

		} ]);




hcvApp.controller('mainCtrl', [ '$scope', 'GenotypeSelection', 'RegionSelection',
function ($scope, GenotypeSelection, RegionSelection) {
	$scope.GenotypeSelection = GenotypeSelection;
	$scope.RegionSelection = RegionSelection;
	$scope.title = "HCV Mutations";  
	$scope.strapline = "Amino-acid level mutation frequencies for aligned Hepatitis C virus sequences";  
	$scope.genotypeSelectHeading = "Select genotype";  
	$scope.genotypeSelectOpen = true;
	$scope.regionSelectHeading = "Select region";  
	$scope.regionSelectOpen = true;
} ]);
