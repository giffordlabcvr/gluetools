projectBrowser.controller('alignmentCtrl', 
		[ '$scope', 'glueWebToolConfig', 'glueWS', 'dialogs', 'pagingContext', 'FileSaver',
		    function($scope, glueWebToolConfig, glueWS, dialogs, pagingContext, FileSaver) {

			addUtilsToScope($scope);
			$scope.memberList = null;
			$scope.renderResult = null;
			$scope.memberWhereClause = null;
			$scope.memberFields = null;
			$scope.loadingSpinner = false;
			$scope.featureTree = null;
			$scope.referenceName = null;
			$scope.selectedNode = null;
			$scope.configuredResult = null;
			
			$scope.downloadAlignment = function(fastaAlignmentExporter, fastaProteinAlignmentExporter) {
				var dlg = dialogs.create(
						glueWebToolConfig.getProjectBrowserURL()+'/dialogs/configureAlignment.html','configureAlignmentCtrl',
						{ featureTree:$scope.featureTree, 
						  initialResult:_($scope.configuredResult).clone(), 
						  initialSelectedNode: $scope.selectedNode }, {});
				dlg.result.then(function(data){
					console.info('data', data);
					$scope.configuredResult = data.result;
					$scope.selectedNode = data.selectedNode;
					
					var cmdParams = {
							recursive:true,
							excludeEmptyRows:true,
							orderStrategy:'increasing_start_segment',
							labelledCodon:false
					};
					var moduleName;
					var fileExtension;
					if($scope.configuredResult.alignmentType == 'nucleotide') {
						moduleName = fastaAlignmentExporter;
						cmdParams.includeAllColumns = false;
						fileExtension = 'fna';
					} else {
						moduleName = fastaProteinAlignmentExporter;
						fileExtension = 'faa';
					}
					console.log("Downloading alignment, using module '"+moduleName+"'");
					cmdParams.alignmentName = $scope.almtName;
					cmdParams.relRefName = $scope.referenceName;
					cmdParams.featureName = $scope.selectedNode.featureName;

					if($scope.configuredResult.regionPart == 'subRegion') {
						if($scope.configuredResult.specifySubregionBy == 'nucleotides') {
							cmdParams.ntRegion = true;
							cmdParams.ntStart = $scope.configuredResult.refStart;
							cmdParams.ntEnd = $scope.configuredResult.refEnd;
						}
						if($scope.configuredResult.specifySubregionBy == 'codons') {
							cmdParams.labelledCodon = true;
							cmdParams.lcStart = $scope.configuredResult.lcStart;
							cmdParams.lcEnd = $scope.configuredResult.lcEnd;
						}
					}
					if($scope.memberWhereClause) {
						cmdParams.whereClause = $scope.memberWhereClause;
					}
					$scope.pagingContext.extendCmdParamsWhereClause(cmdParams);
					if(cmdParams.whereClause != null) {
						cmdParams.allMembers = false;
					} else {
						cmdParams.allMembers = true;
					}

					glueWS.runGlueCommandLong("module/"+moduleName, {
				    	"web-export": cmdParams	
					},
					"Alignment download in progress")
				    .success(function(data, status, headers, config) {
				    	var base64Data;
						if($scope.configuredResult.alignmentType == 'nucleotide') {
							base64Data = data.fastaAlignmentWebExportResult.base64;
						} else {
							base64Data = data.fastaProteinAlignmentWebExportResult.base64;
						}
				    	var blob = $scope.b64ToBlob(base64Data, "text/plain", 512);
					    FileSaver.saveAs(blob, "alignment."+fileExtension);
				    })
				    .error(glueWS.raiseErrorDialog(dialogs, "downloading alignment"));
				});
			}

			$scope.updateCount = function(pContext) {
				$scope.memberList = null;
				$scope.loadingSpinner = true;
				
				var cmdParams = {
		    	        "recursive":"true",
		    			"whereClause": $scope.memberWhereClause
		    	};
				pContext.extendCountCmdParams(cmdParams);

				
				glueWS.runGlueCommand("alignment/"+$scope.almtName, {
			    	"count": { 
			    		"member": cmdParams, 
			    	} 
				})
			    .success(function(data, status, headers, config) {
					console.info('count almt-member raw result', data);
					$scope.pagingContext.setTotalItems(data.countResult.count);
					$scope.pagingContext.firstPage();
			    })
			    .error(function(data, status, headers, config) {
					$scope.loadingSpinner = false;
			    	var fn = glueWS.raiseErrorDialog(dialogs, "counting alignment members");
			    	fn(data, status, headers, config);
			    });
			}
			
			$scope.updatePage = function(pContext) {
				console.log("updatePage", pContext);
				var cmdParams = {
		    	        "recursive":"true",
			            "fieldName":$scope.memberFields,
					    "whereClause": $scope.memberWhereClause
				};
				pContext.extendListCmdParams(cmdParams);
				glueWS.runGlueCommand("alignment/"+$scope.almtName, {
			    	"list": { "member": cmdParams } 
				})
			    .success(function(data, status, headers, config) {
					  $scope.loadingSpinner = false;
					  console.info('list almt-member raw result', data);
					  $scope.memberList = tableResultAsObjectList(data);
					  console.info('list almt-member result as object list', $scope.memberList);
			    })
			    .error(function(data, status, headers, config) {
					$scope.loadingSpinner = false;
			    	var fn = glueWS.raiseErrorDialog(dialogs, "listing alignment members");
			    	fn(data, status, headers, config);
			    });
			}

			$scope.init = function(almtName, almtRendererModuleName, memberWhereClause, memberFields) {
				var renderCmdParams = {};
				$scope.memberFields = memberFields;
				$scope.almtName = almtName;
				$scope.memberWhereClause = memberWhereClause;
				$scope.pagingContext = pagingContext.createPagingContext($scope.updateCount, $scope.updatePage);
				if(almtRendererModuleName) {
					renderCmdParams.rendererModuleName = almtRendererModuleName;
				}
				glueWS.runGlueCommand("alignment/"+almtName, {
				    "render-object":renderCmdParams
				})
				.success(function(data, status, headers, config) {
					$scope.renderResult = data;
					console.info('$scope.renderResult', $scope.renderResult);
					$scope.referenceName = $scope.renderResult.alignment.constrainingReference.name;
					console.log("$scope.referenceName: "+$scope.referenceName);

					glueWS.runGlueCommand("reference/"+$scope.referenceName, {
					    "show":{
					        "feature":{
					            "tree":{}
					        }
					    }
					})
					.success(function(data, status, headers, config) {
						console.info('featureTree', data.referenceFeatureTreeResult);
						$scope.featureTree = data.referenceFeatureTreeResult;
						$scope.pagingContext.countChanged();
					})
					.error(glueWS.raiseErrorDialog(dialogs, "retrieving feature tree"));
					
				})
				.error(glueWS.raiseErrorDialog(dialogs, "rendering alignment"));
			};
			
}]);