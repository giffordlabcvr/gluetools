var glueWS = angular.module('glueWS', ['glueWebToolConfig',
                           		    'angulartics',
                        		    'angulartics.google.analytics']);


glueWS.factory('glueWS', function ($http, glueWebToolConfig, $analytics) {
	var projectURL;
	var urlListenerCallbacks = [];
	return {
		setProjectURL: function(newURL) {
			projectURL = newURL;
		},
		runGlueCommand: function(modePath, command) {
			// logging all glue requests might be overkill? 
			// we could have a boolean used by the client indicating whether
			// to log it.
    		/* $analytics.eventTrack("commandRequest", 
    				{  category: 'glue', 
    					label: 'modePath:'+modePath+
    							',command:'+JSON.stringify(command) }); */
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
				    		$analytics.eventTrack("commandError", 
				    				{  category: 'glue', 
				    					label: 'exceptionName:'+exceptionName+
				    							',exceptionObj:'+JSON.stringify(exceptionObj) });
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
				  dialogs.create(glueWebToolConfig.getGlueWSURL()+'/dialogs/glueErrorDialog.html','glueErrorDialogCtrl',error,{});
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