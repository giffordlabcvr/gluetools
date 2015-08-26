var glueWS = angular.module('glueWS', []);


glueWS.factory('glueWS', function ($http) {
	var projectURL;
	var urlListenerCallbacks = [];
	$http.get('../main/js/glueProjectURL.json').success(function(data) {
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
    });
	return {
		runGlueCommand: function(modePath, command) {
			return $http.post(projectURL+"/"+modePath, command);
		},
		addProjectUrlListener: function(urlListenerCallback) {
			if(projectURL) {
				urlListenerCallback.reportProjectURL(projectURL);
			} else {
				urlListenerCallbacks.push(urlListenerCallback);
			}
		}
	};
});

