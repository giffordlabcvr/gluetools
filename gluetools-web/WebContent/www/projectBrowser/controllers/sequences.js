projectBrowser.controller('sequencesCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 'glueWebToolConfig', 'pagingContext', 'FileSaver', 'saveFile', '$analytics', 'filterUtils',
    function($scope, $route, $routeParams, glueWS, dialogs, glueWebToolConfig, pagingContext, FileSaver, saveFile, $analytics, filterUtils) {

			$scope.listSequenceResult = null;
			$scope.pagingContext = null;
			$scope.whereClause = null;
			$scope.fieldNames = null;
			$scope.loadingSpinner = false;
			$scope.analytics = $analytics;

			addUtilsToScope($scope);
			
			$scope.updateCount = function(pContext) {
				$scope.listSequenceResult = null;
				$scope.loadingSpinner = true;

				var cmdParams = {};
				if($scope.whereClause) {
					cmdParams.whereClause = $scope.whereClause;
				}
				$scope.pagingContext.extendCountCmdParams(cmdParams);
				glueWS.runGlueCommand("", {
			    	"count": { "sequence": cmdParams	 } 
				})
			    .success(function(data, status, headers, config) {
					console.info('count sequence raw result', data);
					$scope.pagingContext.setTotalItems(data.countResult.count);
					$scope.pagingContext.firstPage();
			    })
			    .error(function(data, status, headers, config) {
					$scope.loadingSpinner = false;
			    	var fn = glueWS.raiseErrorDialog(dialogs, "counting sequences");
			    	fn(data, status, headers, config);
			    });
			}
				
			$scope.updatePage = function(pContext) {
				console.log("updatePage", pContext);
				var cmdParams = {
			            "fieldName":$scope.fieldNames
				};
				if($scope.whereClause) {
					cmdParams.whereClause = $scope.whereClause;
				}
				pContext.extendListCmdParams(cmdParams);
				glueWS.runGlueCommand("", {
			    	"list": { "sequence": cmdParams } 
				})
			    .success(function(data, status, headers, config) {
					  console.info('list sequence raw result', data);
					  $scope.listSequenceResult = tableResultAsObjectList(data);
					  console.info('list sequence result as object list', $scope.listSequenceResult);
					  $scope.loadingSpinner = false;
			    })
			    .error(function(data, status, headers, config) {
					$scope.loadingSpinner = false;
			    	var fn = glueWS.raiseErrorDialog(dialogs, "listing sequences");
			    	fn(data, status, headers, config);
			    });
			}

			$scope.init = function(whereClause, fieldNames)  {
				$scope.whereClause = whereClause;
				$scope.fieldNames = fieldNames;
				$scope.pagingContext = pagingContext.createPagingContext($scope.updateCount, $scope.updatePage);
				$scope.pagingContext.countChanged();
			}
			
			$scope.downloadSequences = function(moduleName) {
				saveFile.saveAsDialog("FASTA sequence file", 
						"sequences.fasta",
						function(fileName) {

					console.log("Downloading sequences, using module '"+moduleName+"'");
					var cmdParams = {
							fileName: fileName
					};
					if($scope.whereClause) {
						cmdParams.whereClause = $scope.whereClause;
					}
					$scope.pagingContext.extendCmdParamsWhereClause(cmdParams);
					if(cmdParams.whereClause != null) {
						cmdParams.allSequences = false;
					} else {
						cmdParams.allSequences = true;
					}

					cmdParams.lineFeedStyle = "LF";
					if(userAgent.os.family.indexOf("Windows") !== -1) {
						cmdParams.lineFeedStyle = "CRLF";
					}

					$scope.analytics.eventTrack("sequenceFastaDownload", 
							{   category: 'dataDownload', 
						label: 'totalItems:'+$scope.pagingContext.getTotalItems() });

					glueWS.runGlueCommandLong("module/"+moduleName, {
						"web-export": cmdParams	
					},
					"FASTA sequence file preparation in progress")
					.success(function(data, status, headers, config) {
						var result = data.fastaWebExportResult;
						var dlg = dialogs.create(
								glueWebToolConfig.getProjectBrowserURL()+'/dialogs/fileReady.html','fileReadyCtrl',
								{ 
									url:"gluetools-ws/glue_web_files/"+result.webSubDirUuid+"/"+result.webFileName, 
									fileName: result.webFileName,
									fileSize: result.webFileSizeString
								}, {});
					})
					.error(glueWS.raiseErrorDialog(dialogs, "preparing sequence file"));
				});
			}
			
			
			$scope.downloadSequenceMetadata = function() {
				console.log("Downloading sequence metadata");
				var cmdParams = {
		            "fieldName": $scope.fieldNames
			    };
				if($scope.whereClause) {
					cmdParams.whereClause = $scope.whereClause;
				}
				$scope.pagingContext.extendCmdParamsWhereClause(cmdParams);
				$scope.pagingContext.extendCmdParamsSortOrder(cmdParams);

				var glueHeaders = {
						"glue-binary-table-result" : true,
						"glue-binary-table-result-format" : "TAB",
						"glue-binary-table-line-feed-style" : "LF"
				};

				if(userAgent.os.family.indexOf("Windows") !== -1) {
					glueHeaders["glue-binary-table-line-feed-style"] = "CRLF";
					
				}

				
				$scope.analytics.eventTrack("sequenceMetadataDownload", 
						{   category: 'dataDownload', 
							label: 'totalItems:'+$scope.pagingContext.getTotalItems() });

				glueWS.runGlueCommandLong("", {
			    	"list": {
			    		"sequence" : cmdParams
			    	},
				},
				"Sequence metadata download in progress",
		    	glueHeaders)
			    .success(function(data, status, headers, config) {
			    	var blob = $scope.b64ToBlob(data.binaryTableResult.base64, "text/plain", 512);
				    saveFile.saveFile(blob, "sequence metadata file", "sequence_metadata.txt");
			    })
			    .error(glueWS.raiseErrorDialog(dialogs, "downloading sequence metadata"));
			}
			
			
			
			$scope.globalRegionFilter = function() {
				// note property here is a dummy value.
                return { property:"globalRegion", nullProperty:"who_country", displayName: "Global Region", filterHints: 
	            	{ type: "StringFromFixedValueSet",
                	  generateCustomDefault: function() {
	            		  return {
	            			  fixedValue: $scope.globalRegionFixedValueSet[0]
	            		  };
	            	  },
                	  generatePredicateFromCustom: function(filterElem) {
                		  var custom = filterElem.custom;
                		  var fakeFilterElem = {
                		    property:custom.fixedValue.property,
                		    nullProperty:filterElem.nullProperty,
                			type: "String",
                			predicate: {
                				operator: filterElem.predicate.operator,
                				operand: [custom.fixedValue.value]
                			}
                		  };
                		  var cayennePredicate = filterUtils.filterElemToCayennePredicate(fakeFilterElem);
                		  // we want notmatches here to allow sequences with countries that have a null region/subregion/intregion.
                		  if(filterElem.predicate.operator == 'notmatches') {
                			  cayennePredicate = "( ( "+custom.fixedValue.property + " = null ) or ( " + cayennePredicate + " ) )";
                		  }
                		  return cayennePredicate;
                  	  },
	            	  generateFixedValueSet: function() {
	            		  return $scope.globalRegionFixedValueSet;
	            	  }
	            	}
                };
			};
			$scope.initGlobalRegionFixedValueSet = function () {
				$scope.globalRegionFixedValueSet = [];

				glueWS.runGlueCommand("", {
				    "multi-render":{
				        "tableName":"who_region",
				        "allObjects":"true",
				        "rendererModuleName":"whoRegionTreeRenderer"
				    }
				})
				.success(function(data, status, headers, config) {
					var multiRenderResult = data.multiRenderResult;
					console.info('who region multi-render result', data.multiRenderResult);
					$scope.globalRegionFixedValueSet = [];
					for(var i = 0; i < multiRenderResult.resultDocument.length; i++) {
						var whoRegion = multiRenderResult.resultDocument[i].whoRegion;
						$scope.globalRegionFixedValueSet.push({
							property:"who_country.who_region",
				   			  value:whoRegion.id,
				   			  indent:0,
				   			  displayName:whoRegion.displayName
						});
						if(whoRegion.whoSubRegion != null) {
							for(var j = 0; j < whoRegion.whoSubRegion.length; j++) {
								var whoSubRegion = whoRegion.whoSubRegion[j];
								$scope.globalRegionFixedValueSet.push({
									property:"who_country.who_sub_region",
						   			  value:whoSubRegion.id,
						   			  indent:1,
						   			  displayName:whoSubRegion.displayName
								});
								if(whoSubRegion.whoIntermediateRegion != null) {
									for(var k = 0; k < whoSubRegion.whoIntermediateRegion.length; k++) {
										var whoIntermediateRegion = whoSubRegion.whoIntermediateRegion[k];
										$scope.globalRegionFixedValueSet.push({
											property:"who_country.who_intermediate_region",
								   			  value:whoIntermediateRegion.id,
								   			  indent:2,
								   			  displayName:whoIntermediateRegion.displayName
										});
									}
								}
							}
						}
					}
					console.info('$scope.globalRegionFixedValueSet', $scope.globalRegionFixedValueSet);
				})
				.error(glueWS.raiseErrorDialog(dialogs, "retrieving WHO region tree"));
			};
			
			$scope.developmentStatusFilter = function() {
                // note property here is a dummy value.
                return { property:"developmentStatus", nullProperty:"who_country", displayName: "Country Development Status", filterHints: 
	            	{ type: "StringFromFixedValueSet",
                	  generateCustomDefault: function() {
	            		  return {
	            			  fixedValue: $scope.developmentStatusFixedValueSet[0]
	            		  };
	            	  },
                	  generatePredicateFromCustom: function(filterElem) {
                		  var custom = filterElem.custom;
                		  var type;
                		  if(custom.fixedValue.property == 'who_country.development_status') {
                			  type = "String";
                		  } else {
                			  type = "Boolean";
                		  }
                		  var fakeFilterElem = {
                		    property:custom.fixedValue.property,
                		    nullProperty:filterElem.nullProperty,
                			type: filterElem.type,
                			predicate: {
                				operator: filterElem.predicate.operator,
                				operand: [custom.fixedValue.value]
                			}
                		  };
                		  return filterUtils.filterElemToCayennePredicate(fakeFilterElem);
                  	  },
	            	  generateFixedValueSet: function() {
	            		  return $scope.developmentStatusFixedValueSet;
	            	  }
	            	}
                };
			};
			$scope.initDevelopmentStatusFixedValueSet = function () {
				$scope.developmentStatusFixedValueSet = [
				                        				 {
				                        					 property:"who_country.development_status",
				                        					 value:"developed",
				                        					 indent:0,
				                        					 displayName:"Developed country"
				                        				 },
				                        				 {
				                        					 property:"who_country.development_status",
				                        					 value:"developing",
				                        					 indent:0,
				                        					 displayName:"Developing country"
				                        				 },
				                        				 {
				                        					 property:"who_country.is_ldc",
				                        					 value:"true",
				                        					 indent:1,
				                        					 displayName:"Least developed country (LDC)"
				                        				 },
				                        				 {
				                        					 property:"who_country.is_lldc",
				                        					 value:"true",
				                        					 indent:1,
				                        					 displayName:"Landlocked developing country (LLDC)"
				                        				 },
				                        				 {
				                        					 property:"who_country.is_sids",
				                        					 value:"true",
				                        					 indent:1,
				                        					 displayName:"Small island developing state (SIDS)"
				                        				 },
				                        			];
			};

			
			
			

}]);
