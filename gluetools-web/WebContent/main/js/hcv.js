var hcvApp = angular.module('hcvApp', ['angularTreeview', 'ui.bootstrap']);

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


hcvApp.controller('AccordionDemoCtrl', function ($scope) {
	  $scope.oneAtATime = true;

	  $scope.groups = [
	    {
	      title: 'Dynamic Group Header - 1',
	      content: 'Dynamic Group Body - 1'
	    },
	    {
	      title: 'Dynamic Group Header - 2',
	      content: 'Dynamic Group Body - 2'
	    }
	  ];

	  $scope.items = ['Item 1', 'Item 2', 'Item 3'];

	  $scope.addItem = function() {
	    var newItemNo = $scope.items.length + 1;
	    $scope.items.push('Item ' + newItemNo);
	  };

	  $scope.status = {
	    isFirstOpen: true,
	    isFirstDisabled: false
	  };
	});