projectBrowser.controller('fileConsumerCtrl', [ '$scope', 'glueWS', 'FileUploader', 'dialogs', 'glueWebToolConfig', '$analytics', 'saveFile', 'FileSaver', '$http',
    function($scope, glueWS, FileUploader, dialogs, glueWebToolConfig, $analytics, saveFile, FileSaver, $http) {
	
	
	$scope.removeAll = function() {
		$scope.uploader.clearQueue();
		$scope.fileItemUnderAnalysis = null;
	}

	$scope.removeItem = function(item) {
		if($scope.fileItemUnderAnalysis == item) {
			$scope.fileItemUnderAnalysis = null;
		}
		item.remove();
	}

	var uploader = $scope.uploader = new FileUploader({});
	
}]);
