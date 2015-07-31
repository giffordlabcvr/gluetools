var glueWS = angular.module('glueWS', []);


glueWS.factory('glueWS', function ($http) {
	var projectURL;
	$http.get('../main/js/glueProjectURL.json').success(function(data) {
        projectURL = data.glueProjectURL;
        console.log("Project URL: "+projectURL);
    })
    .error(function(data,status,error,config){
        console.log("Unable to load GLUE project URL: "+data);
    });
	return {
		runGlueCommand: function(modePath, command) {
			return $http.post(projectURL+"/"+modePath, command);
		}
	};
});

