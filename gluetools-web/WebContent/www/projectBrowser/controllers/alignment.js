projectBrowser.controller('alignmentCtrl', 
		[ '$scope', 'glueWebToolConfig', 'glueWS', 'dialogs', 'pagingContext', 'FileSaver', 'saveFile', '$analytics', 'filterUtils',
		    function($scope, glueWebToolConfig, glueWS, dialogs, pagingContext, FileSaver, saveFile, $analytics, filterUtils) {

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
			$scope.analytics = $analytics;

			$scope.downloadAlignment = function(fastaAlignmentExporter, fastaProteinAlignmentExporter) {
				console.info('$scope.featureTree', $scope.featureTree);
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
					if($scope.configuredResult.alignmentType == 'nucleotide') {
						moduleName = fastaAlignmentExporter;
						cmdParams.includeAllColumns = false;
					} else {
						moduleName = fastaProteinAlignmentExporter;
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

					cmdParams.lineFeedStyle = "LF";
					if(userAgent.os.family.indexOf("Windows") !== -1) {
						cmdParams.lineFeedStyle = "CRLF";
					}

					
					$scope.analytics.eventTrack("alignmentDownload", 
							{   category: 'dataDownload', 
								label: 'type:'+$scope.configuredResult.alignmentType+',feature:'+$scope.selectedNode.featureName+',alignment:'+$scope.almtName });
					
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
				    	
				    	saveFile.saveFile(blob, "FASTA alignment file", 
				    			$scope.almtName+"_"+$scope.selectedNode.featureName+"_"+
				    			$scope.configuredResult.alignmentType+"_alignment.fasta");
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
						$scope.featureList = featureTreeToFeatureList($scope.featureTree);
						$scope.pagingContext.countChanged();
					})
					.error(glueWS.raiseErrorDialog(dialogs, "retrieving feature tree"));
					
				})
				.error(glueWS.raiseErrorDialog(dialogs, "rendering alignment"));
			};
			
			
			$scope.downloadMemberMetadata = function() {
				console.log("Downloading clade member metadata");
				var cmdParams = {
		            "fieldName": $scope.memberFields,
		            "recursive": true
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
				
				$scope.analytics.eventTrack("memberMetadataDownload", 
						{   category: 'dataDownload', 
							label: 'alignment:'+$scope.almtName+',totalItems:'+$scope.pagingContext.getTotalItems() });
				glueWS.runGlueCommandLong("alignment/"+$scope.almtName, {
			    	"list": {
			    		"member" : cmdParams
			    	},
				},
				"Clade member metadata download in progress",
		    	glueHeaders)
			    .success(function(data, status, headers, config) {
			    	var blob = $scope.b64ToBlob(data.binaryTableResult.base64, "text/plain", 512);
			    	saveFile.saveFile(blob, "clade member metadata file", $scope.almtName+"_metadata.txt");
			    })
			    .error(glueWS.raiseErrorDialog(dialogs, "downloading clade member metadata"));
			}
			
			$scope.featurePresenceFilter = function() {
                // note property here is a dummy value.
				return { property:"featurePresence", displayName: "Coverage of Genome Region", filterHints: 
            	{ type: "FeaturePresence", 
              	  generateCustomDefault: function() {
              		  return {
              			  feature: $scope.featureList[0], 
              			  minCoveragePct: 90.0
              		  };
              	  },
              	  generatePredicateFromCustom: function(filterElem) {
              		  var custom = filterElem.custom;
              		  var cayennePredicate = 
                		  	"fLocNotes.featureLoc.referenceSequence.name = '"+$scope.referenceName+"' and "+
              		  	"fLocNotes.featureLoc.feature.name = '"+custom.feature.featureName+"' and "+
              		  	"fLocNotes.ref_nt_coverage_pct >= "+custom.minCoveragePct;
              		  return cayennePredicate;
              	  },
              	  getFeaturePresenceFeatures: function() {
              		  if($scope.nonInformationalFeatureList == null && $scope.featureList != null) {
              			  $scope.nonInformationalFeatureList = _.filter($scope.featureList, 
              					  function(f) { return f.featureMetatag == null || f.featureMetatag.indexOf("INFORMATIONAL") < 0;});
              		  }
              		  return($scope.nonInformationalFeatureList);
              	  }
              	}
              };
			}
			
			$scope.globalRegionFilter = function() {
				// note property here is a dummy value.
                return { property:"globalRegion", nullProperty:"sequence.who_country", displayName: "Global Region", filterHints: 
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
							property:"sequence.who_country.who_region",
				   			  value:whoRegion.id,
				   			  indent:0,
				   			  displayName:whoRegion.displayName
						});
						if(whoRegion.whoSubRegion != null) {
							for(var j = 0; j < whoRegion.whoSubRegion.length; j++) {
								var whoSubRegion = whoRegion.whoSubRegion[j];
								$scope.globalRegionFixedValueSet.push({
									property:"sequence.who_country.who_sub_region",
						   			  value:whoSubRegion.id,
						   			  indent:1,
						   			  displayName:whoSubRegion.displayName
								});
								if(whoSubRegion.whoIntermediateRegion != null) {
									for(var k = 0; k < whoSubRegion.whoIntermediateRegion.length; k++) {
										var whoIntermediateRegion = whoSubRegion.whoIntermediateRegion[k];
										$scope.globalRegionFixedValueSet.push({
											property:"sequence.who_country.who_intermediate_region",
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
                return { property:"developmentStatus", nullProperty:"sequence.who_country", displayName: "Country Development Status", filterHints: 
	            	{ type: "StringFromFixedValueSet",
                	  generateCustomDefault: function() {
	            		  return {
	            			  fixedValue: $scope.developmentStatusFixedValueSet[0]
	            		  };
	            	  },
                	  generatePredicateFromCustom: function(filterElem) {
                		  var custom = filterElem.custom;
                		  var type;
                		  if(custom.fixedValue.property == 'sequence.who_country.development_status') {
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
				                        					 property:"sequence.who_country.development_status",
				                        					 value:"developed",
				                        					 indent:0,
				                        					 displayName:"Developed country"
				                        				 },
				                        				 {
				                        					 property:"sequence.who_country.development_status",
				                        					 value:"developing",
				                        					 indent:0,
				                        					 displayName:"Developing country"
				                        				 },
				                        				 {
				                        					 property:"sequence.who_country.is_ldc",
				                        					 value:"true",
				                        					 indent:1,
				                        					 displayName:"Least developed country (LDC)"
				                        				 },
				                        				 {
				                        					 property:"sequence.who_country.is_lldc",
				                        					 value:"true",
				                        					 indent:1,
				                        					 displayName:"Landlocked developing country (LLDC)"
				                        				 },
				                        				 {
				                        					 property:"sequence.who_country.is_sids",
				                        					 value:"true",
				                        					 indent:1,
				                        					 displayName:"Small island developing state (SIDS)"
				                        				 },
				                        			];
			};
			
}]);