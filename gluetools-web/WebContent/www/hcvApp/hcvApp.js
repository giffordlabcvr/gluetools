console.log("before hcvApp module definition");

var hcvApp = angular.module('hcvApp', [
    'ngRoute',
    'analysisTool', 
    'projectBrowser', 
    'home',
    'glueWS',
    'glueWebToolConfig',
    'treeControl'
  ]);

console.log("after hcvApp module definition");

hcvApp.config(['$routeProvider', 'projectBrowserStandardRoutesProvider',
  function($routeProvider, projectBrowserStandardRoutesProvider) {
	
	var projectBrowserStandardRoutes = projectBrowserStandardRoutesProvider.$get();
	var projectBrowserURL = "../projectBrowser";

	projectBrowserStandardRoutes.addReferencesRoute($routeProvider, projectBrowserURL);
	projectBrowserStandardRoutes.addReferenceRoute($routeProvider, projectBrowserURL);
	projectBrowserStandardRoutes.addSequencesRoute($routeProvider, projectBrowserURL);
	projectBrowserStandardRoutes.addSequenceRoute($routeProvider, projectBrowserURL);
	//projectBrowserStandardRoutes.addAlignmentsRoute($routeProvider, projectBrowserURL);
    // custom alignments view
	$routeProvider.
    when('/project/alignment', {
  	  templateUrl: 'views/hcvAlignments.html',
  	  controller: 'hcvAlignmentsCtrl'
      });
	// drugs overview
	$routeProvider.
    when('/project/drug', {
    	  templateUrl: 'views/hcvDrugs.html',
    	  controller: 'hcvDrugsCtrl'
        });
	// specific drug
	$routeProvider.
    when('/project/drug/:id', {
    	  templateUrl: 'views/hcvDrug.html',
    	  controller: 'hcvDrugCtrl'
        });
	// Drug resistance publications overview
	$routeProvider.
    when('/project/drug_resistance_publication', {
    	  templateUrl: 'views/hcvDrugResistancePublications.html',
    	  controller: 'hcvDrugResistancePublicationsCtrl'
        });
	// specific drug resistance publication
	$routeProvider.
    when('/project/drug_resistance_publication/:id', {
    	  templateUrl: 'views/hcvDrugResistancePublication.html',
    	  controller: 'hcvDrugResistancePublicationCtrl'
        });
	// specific RAV
	$routeProvider.
    when('/project/rav/:referenceName/:featureName/:variationName', {
    	  templateUrl: 'views/hcvRav.html',
    	  controller: 'hcvRavCtrl'
        });
	// RAVs overview
	$routeProvider.
    when('/project/rav', {
    	  templateUrl: 'views/hcvRavs.html',
    	  controller: 'hcvRavsCtrl'
        });
	// custom single alignment view
	$routeProvider.
    when('/project/alignment/:alignmentName', {
	  templateUrl: 'views/hcvAlignment.html',
	  controller: 'hcvAlignmentCtrl'
    });
	projectBrowserStandardRoutes.addAlignmentMemberRoute($routeProvider, projectBrowserURL);
	
    $routeProvider.
      when('/analysisTool', {
        templateUrl: '../analysisTool/analysisTool.html',
        controller: 'analysisToolCtrl'
      }).
      when('/home', {
    	  templateUrl: './modules/home/home.html',
    	  controller: 'homeCtrl'
      }).
      otherwise({
    	  redirectTo: '/home'
      });
}]);

hcvApp.controller('hcvAppCtrl', 
  [ '$scope', 'glueWS', 'glueWebToolConfig',
function ($scope, glueWS, glueWebToolConfig) {
	$scope.brand = "HCV-GLUE";
	$scope.homeMenuTitle = "Home";
	$scope.analysisMenuTitle = "Analysis";
	$scope.analysisToolMenuTitle = "Sequence Typing and Interpretation";
	$scope.projectBrowserMenuTitle = "Sequence Database";
	$scope.drugResistanceMenuTitle = "Drug Resistance";
	$scope.projectBrowserAlignmentMenuTitle = "Clade Tree";
	$scope.projectBrowserDrugMenuTitle = "Direct-acting Antivirals";
	$scope.projectBrowserRavMenuTitle = "Resistance-associated Substitutions";
	$scope.projectBrowserDrugPubMenuTitle = "Drug Resistance References";
	$scope.projectBrowserReferenceSequenceMenuTitle = "Reference Sequences";
	$scope.projectBrowserSequenceMenuTitle = "Sequences";
	glueWS.setProjectURL("../../../gluetools-ws/project/hcv");
	glueWebToolConfig.setAnalysisToolURL("../analysisTool");
	glueWebToolConfig.setProjectBrowserURL("../projectBrowser");
	glueWebToolConfig.setGlueWSURL("../glueWS");
	glueWebToolConfig.setRendererDialogs([
	                              	    {
	                            	    	renderer: "hcvEpitopeRenderer",
	                            	    	dialogURL: "dialogs/displayEpitope.html",
	                            	    	dialogController: "displayEpitopeCtrl"
	                            	    },
	                            	    {
	                            	    	renderer: "hcvCommonAaPolymorphismRenderer",
	                            	    	dialogURL: "dialogs/displayCommonAa.html",
	                            	    	dialogController: "displayCommonAaCtrl"
	                            	    },
	                            	    {
	                            	    	renderer: "hcvResistanceAssociatedVariantRenderer",
	                            	    	dialogURL: "dialogs/displayRAV.html",
	                            	    	dialogController: "displayRAVCtrl"
	                            	    }
	]);
} ]);


