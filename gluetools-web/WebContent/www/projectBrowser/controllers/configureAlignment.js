projectBrowser.controller('configureAlignmentCtrl',
		[ '$scope', '$modalInstance', 'data',
	function($scope, $modalInstance, data){
	$scope.data = data;
	$scope.selectedNode = null
	$scope.result = {
		alignmentType:'nucleotide',
		regionPart:'wholeRegion',
		specifySubregionBy:'nucleotides',
		refStart:null,
		refEnd:null,
		lcStart:null,
		lcEnd:null,
		featureName:null,
	};

	$scope.$watch('selectedNode', function() {
		if($scope.selectedNode != null) {
			$scope.result.alignmentType = 'nucleotide';
			$scope.result.regionPart = 'wholeRegion';
			$scope.result.specifySubregionBy = 'nucleotides';
			$scope.result.featureName = $scope.selectedNode.featureName;
		}
	});

	$scope.$watch('result.alignmentType', function() {
		if($scope.result.alignmentType == 'aminoAcid') {
			$scope.result.specifySubregionBy = 'codons';
		}
		if($scope.result.alignmentType == 'nucleotide') {
			$scope.result.specifySubregionBy = 'nucleotides';
		}
	});
	
	$scope.accept = function(){
		$modalInstance.close($scope.result);
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

}]);