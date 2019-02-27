var gluetoolsApp = angular.module('gluetoolsApp', [
  'glueWS', 
  'glueWebToolConfig', 
  'treeControl',
  'angulartics',
  'angulartics.google.analytics',
  'ui.bootstrap',
  'dialogs.main',
  'ngRoute',
  'hljs'
  ]);

gluetoolsApp.config(['$routeProvider',
    function($routeProvider) {
      $routeProvider.
      	when('/notFound', {
      		templateUrl: './pages/notFound/page.html',
      		controller: 'notFoundCtrl'
      	}).
        when('/home', {
            templateUrl: './pages/home/page.html',
            controller: 'homeCtrl'
          }).
      	when('/overview', {
            templateUrl: './pages/design/overview/page.html',
            controller: 'overviewCtrl'
          }).
      	when('/coreSchema', {
            templateUrl: './pages/design/coreSchema/page.html',
            controller: 'coreSchemaCtrl'
          }).
      	when('/alignments', {
            templateUrl: './pages/design/alignments/page.html',
            controller: 'alignmentsCtrl'
          }).
    	when('/commandLayer', {
            templateUrl: './pages/design/commandLayer/page.html',
            controller: 'commandLayerCtrl'
          }).
      	when('/softwareArchitecture', {
            templateUrl: './pages/design/softwareArchitecture/page.html',
            controller: 'softwareArchitectureCtrl'
          }).
      	when('/webService', {
            templateUrl: './pages/design/webService/page.html',
            controller: 'webServiceCtrl'
          }).
      	when('/introduction', {
            templateUrl: './pages/userGuide/introduction/page.html',
            controller: 'introductionCtrl'
          }).
      	when('/installation', {
            templateUrl: './pages/userGuide/installation/page.html',
            controller: 'installationCtrl'
          }).
    	when('/exampleProject', {
            templateUrl: './pages/userGuide/exampleProject/page.html',
            controller: 'exampleProjectCtrl'
          }).
    	when('/interactiveCommandLine', {
            templateUrl: './pages/userGuide/interactiveCommandLine/page.html',
            controller: 'interactiveCommandLineCtrl'
          }).
      	when('/invokingGlueFromTheShell', {
            templateUrl: './pages/userGuide/invokingGlueFromTheShell/page.html',
            controller: 'invokingGlueFromTheShellCtrl'
          }).
      	when('/buildYourOwnProject', {
            templateUrl: './pages/userGuide/buildYourOwnProject/page.html',
            controller: 'buildYourOwnProjectCtrl'
          }).
      	when('/managingAProject', {
            templateUrl: './pages/userGuide/managingAProject/page.html',
            controller: 'managingAProjectCtrl'
          }).
    	when('/queryingGlue', {
            templateUrl: './pages/userGuide/queryingGlue/page.html',
            controller: 'queryingGlueCtrl'
          }).
      	when('/modules', {
            templateUrl: './pages/userGuide/modules/page.html',
            controller: 'modulesCtrl'
          }).
      	when('/scriptingLayer', {
            templateUrl: './pages/userGuide/scriptingLayer/page.html',
            controller: 'scriptingLayerCtrl'
          }).
    	when('/schemaExtensions', {
            templateUrl: './pages/userGuide/schemaExtensions/page.html',
            controller: 'schemaExtensionsCtrl'
          }).
    	when('/freemarkerTemplates', {
            templateUrl: './pages/userGuide/freemarkerTemplates/page.html',
            controller: 'freemarkerTemplatesCtrl'
          }).
    	when('/buildingAnAlignmentTree', {
            templateUrl: './pages/userGuide/buildingAnAlignmentTree/page.html',
            controller: 'buildingAnAlignmentTreeCtrl'
          }).
        when('/maximumLikelihoodCladeAssignment', {
	        templateUrl: './pages/userGuide/maximumLikelihoodCladeAssignment/page.html',
	        controller: 'maximumLikelihoodCladeAssignmentCtrl'
	      }).
    	when('/workingWithGenbankData', {
            templateUrl: './pages/userGuide/workingWithGenbankData/page.html',
            controller: 'workingWithGenbankDataCtrl'
          }).
      	when('/variations', {
            templateUrl: './pages/userGuide/variations/page.html',
            controller: 'variationsCtrl'
          }).
    	when('/deepSequencingData', {
            templateUrl: './pages/userGuide/deepSequencingData/page.html',
            controller: 'deepSequencingDataCtrl'
          }).
      	when('/commandModes', {
            templateUrl: './pages/referenceDocumentation/commandModes/page.html',
            controller: 'commandModesCtrl'
          }).
    	when('/commandModes/commandMode/:absoluteModePathID', {
            templateUrl: './pages/referenceDocumentation/commandModes/commandMode/page.html',
            controller: 'commandModeCtrl'
          }).
    	when('/commandModes/commandMode/:absoluteModePathID/command/:cmdWordID', {
            templateUrl: './pages/referenceDocumentation/commandModes/commandMode/command/page.html',
            controller: 'commandCtrl'
          }).
      	when('/nonModeCommands', {
            templateUrl: './pages/referenceDocumentation/nonModeCommands/page.html',
            controller: 'nonModeCommandsCtrl'
          }).
      	when('/nonModeCommands/command/:cmdWordID', {
            templateUrl: './pages/referenceDocumentation/nonModeCommands/command/page.html',
            controller: 'nonModeCommandCtrl'
          }).
        when('/moduleReference/moduleType/:name', {
              templateUrl: './pages/referenceDocumentation/moduleReference/moduleType/page.html',
              controller: 'moduleTypeCtrl'
          }).
        when('/moduleReference/moduleType/:name/command/:cmdWordID', {
            templateUrl: './pages/referenceDocumentation/moduleReference/moduleCommand/page.html',
            controller: 'moduleCommandCtrl'
          }).
      	when('/moduleReference', {
            templateUrl: './pages/referenceDocumentation/moduleReference/page.html',
            controller: 'moduleReferenceCtrl'
          }).
      	when('/download', {
            templateUrl: './pages/download/page.html',
            controller: 'downloadCtrl'
          }).
      	when('/about', {
            templateUrl: './pages/about/page.html',
            controller: 'aboutCtrl'
          }).
        when('/', {
            redirectTo: '/home'
        }).
        otherwise({
            redirectTo: '/notFound'
        });
    }]);

gluetoolsApp.controller('gluetoolsCtrl', 
[ '$scope', 'glueWS', 'glueWebToolConfig',
  function ($scope, glueWS, glueWebToolConfig) {

	glueWS.setProjectURL("../../../gluetools-ws");
	glueWebToolConfig.setGlueWSURL("../gluetools-web/www/glueWS");
	
  	$scope.brand = "GLUE";
  	$scope.homeMenuTitle = "Home";

  	$scope.designMenuTitle = "Design";
  	$scope.overviewMenuTitle = "Design Overview";
  	$scope.coreSchemaMenuTitle = "Core Schema";
  	$scope.alignmentTreeMenuTitle = "Alignments and Alignment Trees";
  	$scope.commandLayerMenuTitle = "Command Layer";
  	$scope.softwareArchitectureMenuTitle = "Software Architecture";
  	$scope.webServiceMenuTitle = "Web Service";

  	$scope.userGuideMenuTitle = "User guide";
  	$scope.introductionMenuTitle = "Introduction";
  	$scope.installationMenuTitle = "Installing GLUE";
  	$scope.exampleProjectMenuTitle = "Example GLUE Project";
  	$scope.interactiveCommandLineMenuTitle = "Command Line Interpreter";
    $scope.invokingGlueFromTheShellMenuTitle = "Invoking GLUE as a Unix command"
    $scope.buildYourOwnProjectMenuTitle = "Build your own project";
    $scope.managingAProjectMenuTitle = "Managing a project";
    $scope.queryingGlueMenuTitle = "Querying the GLUE database";
    $scope.modulesMenuTitle = "Module system";
    $scope.scriptingLayerMenuTitle = "Scripting Layer";
    $scope.schemaExtensionsMenuTitle = "Schema Extensions";
    $scope.freemarkerTemplatesMenuTitle = "Freemarker templates";
  	$scope.deepSequencingDataMenuTitle = "Working with deep sequencing data";
    $scope.variationsMenuTitle = "Variations";
    $scope.buildingAnAlignmentTreeMenuTitle = "Building an Alignment Tree";
    $scope.maximumLikelihoodCladeAssignmentMenuTitle = "Maximum Likelihood Clade Assignment";
    $scope.workingWithGenbankDataMenuTitle = "Working with sequence data from GenBank";
  	$scope.referenceDocumentationMenuTitle = "Reference documentation";
  	$scope.commandModesMenuTitle = "Command mode reference";
  	$scope.nonModeCommandsMenuTitle = "Non-mode-specific command reference"
  	$scope.moduleReferenceMenuTitle = "Module type reference";

  	$scope.downloadMenuTitle = "Download GLUE";
  	$scope.aboutMenuTitle = "About GLUE";

} ]);

