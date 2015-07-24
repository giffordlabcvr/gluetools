var hcvApp = angular.module('hcvApp', ['angularTreeview']);

hcvApp.controller('searchCtrl', [ '$scope', '$http', function($scope, $http) {
		
	$scope.genotypes = null
    $http.get('../main/js/hcvGenotypes.json')
        .success(function(data) {
            $scope.genotypes = [ data ];
        })
        .error(function(data,status,error,config){
            $scope.contents = [{heading:"Error",description:"Could not load json data"}];
        });
	
	$scope.$watch( 'genotypesTree.currentNode', function( newObj, oldObj ) {
	    if( $scope.genotypesTree && angular.isObject($scope.genotypesTree.currentNode) ) {
	        $scope.genotypeID = $scope.genotypesTree.currentNode.id;
	    }
	}, false);

} ]);