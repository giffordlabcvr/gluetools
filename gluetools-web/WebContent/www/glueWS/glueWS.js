var glueWS = angular.module('glueWS', ['moduleURLs']);


glueWS.factory('glueWS', function ($http, moduleURLs) {
	var projectURL;
	var urlListenerCallbacks = [];
	/*$http.get('../main/js/hcvApp/glueProjectURL.json').success(function(data) {
        projectURL = data.glueProjectURL;
        console.log("Project URL: "+projectURL);
        for(var i = 0; i < urlListenerCallbacks.length; i++) {
            console.log("callback: "+i);
        	urlListenerCallbacks[i].reportProjectURL(projectURL);
        }
        urlListenerCallbacks = [];
    })
    .error(function(data,status,error,config){
        console.log("Unable to load GLUE project URL: "+data);
    });*/
	return {
		setProjectURL: function(newURL) {
			projectURL = newURL;
		},
		runGlueCommand: function(modePath, command) {
			return $http.post(projectURL+"/"+modePath, command);
		},
		addProjectUrlListener: function(urlListenerCallback) {
			if(projectURL) {
				urlListenerCallback.reportProjectURL(projectURL);
			} else {
				urlListenerCallbacks.push(urlListenerCallback);
			}
		},
		raiseErrorDialog: function(dialogs, activityErrorOccurredIn) {
			  var bodyHeadline = "GLUE error while "+activityErrorOccurredIn+":";
			  return function(data, status, headers, config) {
				  console.log("GLUE error while "+activityErrorOccurredIn+":"+JSON.stringify(data));
				  var error = {
						  title : "GLUE error",
						  bodyHeadline: bodyHeadline,
						  glueMessage : "HTTP response status: "+status,
						  showItems : false
				  };
				  if(data && _.isObject(data)) {
					  var pairs = _.pairs(data);
					  if(pairs.length > 0) {
						  var exceptionName = pairs[0][0];
						  var exceptionObj = pairs[0][1];
						  if(exceptionObj && _.isObject(exceptionObj)) {
							  var objMessage = exceptionObj.message;
							  if(objMessage) {
								  error.glueMessage = objMessage;
							  }
							  error.showItems = true;
							  error.items = [ {name:"exceptionName", value:exceptionName},
							                  {name:"code", value: exceptionObj.code} ];
							  var objPairs = _.pairs(exceptionObj);
							  for(var i = 0; i < objPairs.length; i++) {
								  if(objPairs[i][0] != "code" && objPairs[i][0] != "message") {
									  error.items.push({name: objPairs[i][0], value: objPairs[i][1]});
								  }
							  }
							  
						  }
					  }
				  }
				  dialogs.create(moduleURLs.getGlueWSURL()+'/dialogs/glueErrorDialog.html','glueErrorDialogCtrl',error,{});
			  }
		}
	};
});

glueWS.controller('glueErrorDialogCtrl',function($scope,$modalInstance,data){
	$scope.error = data;
	
	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 
})