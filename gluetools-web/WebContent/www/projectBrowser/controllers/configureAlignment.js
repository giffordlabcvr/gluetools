projectBrowser.controller('configureAlignmentCtrl',
		[ '$scope', '$modalInstance', 'data',
	function($scope, $modalInstance, data){
	$scope.data = data;
	$scope.inited = false;
	if(data.initialSelectedNode != null) {
		$scope.selectedNode = data.initialSelectedNode;
	} else {
		$scope.selectedNode = null;
	}
	
	$scope.defaultResult = function() {
		return {
			alignmentType:'nucleotide',
			regionPart:'wholeRegion',
			specifySubregionBy:'nucleotides',
			refStart:null,
			refEnd:null,
			lcStart:null,
			lcEnd:null
		};
	}

	console.log("data.initialResult", data.initialResult);
	if(data.initialResult != null) {
		$scope.result = data.initialResult;
	} else {
		$scope.result = $scope.defaultResult();
	}
	
	$scope.inited = true;

	$scope.accept = function(){
		// correct config parts.
		if($scope.selectedNode.featureMetatag.indexOf('CODES_AMINO_ACIDS') < 0) {
			$scope.result.alignmentType = 'nucleotide';
	 	}
		if($scope.result.regionPart == 'subRegion' && $scope.result.alignmentType == 'aminoAcid') {
			$scope.result.specifySubregionBy = 'codons';
		}
		$modalInstance.close({
			result:$scope.result,
			selectedNode:$scope.selectedNode
		});
	}; 

	$scope.resetToDefault = function(){
		$scope.selectedNode = $scope.data.featureTree.features[0];
		$scope.result = $scope.defaultResult();
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

}]);