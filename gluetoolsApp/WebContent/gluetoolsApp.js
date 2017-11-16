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
      	when('/alignmentTree', {
            templateUrl: './pages/design/alignmentTree/page.html',
            controller: 'alignmentTreeCtrl'
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
      	when('/interactiveCommandLine', {
            templateUrl: './pages/userGuide/interactiveCommandLine/page.html',
            controller: 'interactiveCommandLineCtrl'
          }).
      	when('/invokingGlueFromTheShell', {
            templateUrl: './pages/userGuide/invokingGlueFromTheShell/page.html',
            controller: 'invokingGlueFromTheShellCtrl'
          }).
      	when('/projectStructure', {
            templateUrl: './pages/userGuide/projectStructure/page.html',
            controller: 'projectStructureCtrl'
          }).
      	when('/managingAProject', {
            templateUrl: './pages/userGuide/managingAProject/page.html',
            controller: 'managingAProjectCtrl'
          }).
      	when('/cayenneQueries', {
            templateUrl: './pages/userGuide/cayenneQueries/page.html',
            controller: 'cayenneQueriesCtrl'
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
    	when('/commandReference', {
            templateUrl: './pages/referenceDocumentation/commandReference/page.html',
            controller: 'commandReferenceCtrl'
          }).
      	when('/moduleReference', {
            templateUrl: './pages/referenceDocumentation/moduleReference/page.html',
            controller: 'moduleReferenceCtrl'
          }).
    	when('/download', {
            templateUrl: './pages/download/page.html',
            controller: 'downloadCtrl'
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
  	$scope.alignmentTreeMenuTitle = "Alignment Tree";
  	$scope.commandLayerMenuTitle = "Command Layer";
  	$scope.softwareArchitectureMenuTitle = "Software Architecture";
  	$scope.webServiceMenuTitle = "Web Service";

  	$scope.userGuideMenuTitle = "User guide";
  	$scope.introductionMenuTitle = "Introduction";
  	$scope.installationMenuTitle = "Installing GLUE";
  	$scope.interactiveCommandLineMenuTitle = "Interactive command line";
    $scope.invokingGlueFromTheShellMenuTitle = "Invoking GLUE from the shell"
    $scope.projectStructureMenuTitle = "Project structure";
    $scope.managingAProjectMenuTitle = "Managing a project";
    $scope.cayenneQueriesMenuTitle = "Cayenne queries";
    $scope.modulesMenuTitle = "Modules";
    $scope.scriptingLayerMenuTitle = "Scripting Layer";
    $scope.schemaExtensionsMenuTitle = "Schema Extensions";
    $scope.freemarkerTemplatesMenuTitle = "Freemarker templates";

  	$scope.referenceDocumentationMenuTitle = "Reference documentation";
  	$scope.commandReferenceMenuTitle = "Command reference";
  	$scope.moduleReferenceMenuTitle = "Module reference";

    
  	$scope.downloadMenuTitle = "Download GLUE";

} ]);

