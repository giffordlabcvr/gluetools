var mutationsBrowser = angular.module('mutationsBrowser', ['angularTreeview', 'ui.bootstrap']);

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
		[ '$scope', '$http', 'Mutations',
		function($scope, $http, Mutations) {
			$scope.Mutations = Mutations;

			$scope.aasPerRow = 20;
			
			$scope.mutationRows = [];
			$scope.$watch(function () { return Mutations.getMutations(); }, function( newObj, oldObj ) {
				if(newObj != oldObj) {
					$scope.mutationRows = [];
					var startIndex = 0;
					while(startIndex < newObj.length) {
						var rowLength = Math.min($scope.aasPerRow, newObj.length - startIndex);
						var mutationRow = {};
						mutationRow.consensusIndices = [];
						mutationRow.consensusAAs = [];
						mutationRow.numIsolatesList = [];
						mutationRow.mutations = [];
						for(var columnIndex = startIndex; columnIndex < startIndex+rowLength; columnIndex++) {
							var aa = newObj[columnIndex];
							if((columnIndex+1) % 10 == 0) {
								mutationRow.consensusIndices.push(columnIndex+1);
							} else {
								mutationRow.consensusIndices.push(String.fromCharCode(160));
							}
							mutationRow.consensusAAs.push(aa.consensusAA);
							mutationRow.numIsolatesList.push(aa.numIsolates);
							while(mutationRow.mutations.length < aa.mutations.length) {
								var array = [];
								for(var i = 0; i < rowLength; i++) { array.push({}); }
								mutationRow.mutations.push(array);
							}
							for(var mutIndex = 0; mutIndex < aa.mutations.length; mutIndex++) {
								mutationRow.mutations[mutIndex][columnIndex-startIndex] = {
										"mutationAA": aa.mutations[mutIndex].mutationAA, 
										"mutationPercent": toFixed(aa.mutations[mutIndex].isolatesPercent, 1)
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




mutationsBrowser.controller('mutationsBrowserCtrl', [ '$scope', 
             'GenotypeSelection', 'RegionSelection', 'Mutations',
function ($scope, GenotypeSelection, RegionSelection, Mutations) {
	$scope.GenotypeSelection = GenotypeSelection;
	$scope.RegionSelection = RegionSelection;
	$scope.Mutations = Mutations;
	$scope.genotypeSelectHeading = "Select sequence genotype";  
	$scope.genotypeSelectOpen = false;
	$scope.regionSelectHeading = "Select genome region";  
	$scope.regionSelectOpen = false;
	
	$scope.updateMutations = function() {
		$scope.Mutations.setMutations($scope.generateRandomMutFreqs());
		//console.log("Mutations: "+JSON.stringify($scope.Mutations.getMutations()));
	};
	
	
	$scope.generateRandomMutFreqs = function() {
		var aas = [];
		var numAAs = 65;
		var minNumIsolates = 4000;
		var maxNumIsolates = 14000;
		var mutationChance = 0.25;
		for(var aaIndex = 0; aaIndex < numAAs; aaIndex++) {
			var mutations = [];
			var percentage = 49.9;
			while(Math.random() < mutationChance) {
				var mutPercentage = Math.random() * percentage;
				if(mutPercentage > 1.0) {
					mutations.push({
						"mutationAA": $scope.randomAA(),
						"isolatesPercent": mutPercentage
					});
					percentage = mutPercentage / 2.0;
				}
			}
			aas.push({
				"consensusAA": $scope.randomAA(),
				"numIsolates": minNumIsolates+ Math.floor(Math.random() * (maxNumIsolates - minNumIsolates)),
				"mutations": mutations
			});
		}
		return aas;
	}
	
	
	$scope.randomAA = function() {
		return 'ACDEFGHIKLMNOPQRSTUVWY'[Math.floor(Math.random() * 22)];
	}
	
} ]);




