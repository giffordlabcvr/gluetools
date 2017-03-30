projectBrowser.controller('configureAlignmentCtrl',
		[ '$scope', '$modalInstance', 'data',
	function($scope, $modalInstance, data){
	$scope.data = data;
	$scope.selectedNode = null
	$scope.result = {
		alignmentType:'nucleotide',
		regionPart:'wholeRegion',
		refStart:null,
		refEnd:null,
		lcStart:null,
		lcEnd:null,
		featureName:null
	};
	
	$scope.accept = function(){
		$scope.result.featureName = $scope.selectedNode.featureName;
		$modalInstance.close($scope.result);
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

}]);