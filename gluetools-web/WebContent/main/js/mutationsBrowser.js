var mutationsBrowser = angular.module('mutationsBrowser', ['angularTreeview', 'ui.bootstrap', 'glueWS']);

mutationsBrowser.factory('GenotypeSelection', function (){
	return {
		id: "",
		label: "",
		sequences: [], 
		showSequences: false
	};
});

mutationsBrowser.factory('RegionSelection', function (){
	return {
		id: "",
		label: "", 
		description: ""
	};
});

mutationsBrowser.factory('Mutations', function (){
	var mutations = [];
	return {
		getMutations: function() {
			return mutations;
		}, 
		setMutations: function(newMutations) {
			mutations = newMutations;
		}
	};
});



mutationsBrowser.controller('selectGenotypeCtrl', 
		[ '$scope', '$http', 'GenotypeSelection',
		function($scope, $http, GenotypeSelection) {
			$scope.GenotypeSelection = GenotypeSelection;

			$scope.defaultOpenDepth = 1;
			$scope.defaultSelectedId = "all";
			
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


mutationsBrowser.controller('selectRegionCtrl', 
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

mutationsBrowser.controller('mutationTableCtrl', 
		[ '$scope', 'Mutations',
		function($scope, Mutations) {
			$scope.Mutations = Mutations;

			$scope.aasPerRow = 20;
			
			$scope.mutationRows = [];
			$scope.$watch(function () { return Mutations.getMutations(); }, function( newObj, oldObj ) {
				if(newObj != oldObj) {
					$scope.mutationRows = [];
					var startIndex = 0;
					var aaLocusList = newObj.mutationSet.aaLocus;
					while(startIndex < aaLocusList.length) {
						var rowLength = Math.min($scope.aasPerRow, aaLocusList.length - startIndex);
						var mutationRow = {};
						mutationRow.consensusIndices = [];
						mutationRow.consensusAAs = [];
						mutationRow.numIsolatesList = [];
						mutationRow.mutations = [];
						for(var columnIndex = startIndex; columnIndex < startIndex+rowLength; columnIndex++) {
							var aa = aaLocusList[columnIndex];
							if((columnIndex+1) % 10 == 0) {
								mutationRow.consensusIndices.push(columnIndex+1);
							} else {
								mutationRow.consensusIndices.push(String.fromCharCode(160));
							}
							mutationRow.consensusAAs.push(aa.consensusAA);
							mutationRow.numIsolatesList.push(aa.numIsolates);
							if(aa.mutation) {
								while(mutationRow.mutations.length < aa.mutation.length) {
									var array = [];
									for(var i = 0; i < rowLength; i++) { array.push({}); }
									mutationRow.mutations.push(array);
								}
							}
							if(aa.mutation) {
								for(var mutIndex = 0; mutIndex < aa.mutation.length; mutIndex++) {
									mutationRow.mutations[mutIndex][columnIndex-startIndex] = {
											"mutationAA": aa.mutation[mutIndex].mutationAA, 
											"mutationPercent": toFixed(aa.mutation[mutIndex].isolatesPercent, 1)
									}
								}
							}
						}
						var array = [];
						for(var i = 0; i < rowLength; i++) {
							array.push({"mutationAA": String.fromCharCode(160)});
						}
						mutationRow.mutations.push(array);
						$scope.mutationRows.push(mutationRow);
						startIndex = startIndex + $scope.aasPerRow;
					}
				}
			}, false);

		} ]);




mutationsBrowser.controller('mutationsBrowserCtrl', [ '$scope', 'glueWS', 
             'GenotypeSelection', 'RegionSelection', 'Mutations',
function ($scope, glueWS, GenotypeSelection, RegionSelection, Mutations) {
	$scope.GenotypeSelection = GenotypeSelection;
	$scope.RegionSelection = RegionSelection;
	$scope.Mutations = Mutations;
	$scope.genotypeSelectHeading = "Select sequence genotype";  
	$scope.genotypeSelectOpen = false;
	$scope.regionSelectHeading = "Select genome region";  
	$scope.regionSelectOpen = false;
	
	$scope.updateMutations = function() {
		var glueCommand = {"generate": {
			"taxon":GenotypeSelection.id,
			"feature":RegionSelection.id
		}}
		// console.log(JSON.stringify(glueCommand));
		glueWS.runGlueCommand('module/mutationFrequencies', glueCommand).
		success(function(data, status, headers, config) {
			  //console.log("HTTP POST response body: "+JSON.stringify(data));
			  $scope.Mutations.setMutations(data);
			  //console.log("Mutations: "+JSON.stringify($scope.Mutations.getMutations()));
		  }).
		  error(function(data, status, headers, config) {
			  console.log("HTTP POST error: "+JSON.stringify(data));
		  });
		
		
	};
	
	
	
} ]);




