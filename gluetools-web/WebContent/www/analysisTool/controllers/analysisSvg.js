analysisTool.controller('analysisSvg', ['$scope', 'glueWS', 'dialogs', 'glueWebToolConfig', 
                                        function($scope, glueWS, dialogs, glueWebToolConfig) {
	
	$scope.analysisToolURL = glueWebToolConfig.getAnalysisToolURL();
	
	$scope.svgParams = {
			sequenceLabelWidth: 150,
			ntWidth: 16,
			ntHeight: 16,
			ntIndexHeight: 55,
			ntGap: 4,
			codonLabelHeight: 35,
			aaHeight: 25,
			varHeight: 20,
			pLocConnectorHeight: 8,
			varTooltipHeight: 30,
			varTooltipGap: 10,
			varTooltipExtraWidth: 10,
			varGap: 5,
			numVarTracks: 0,
			
			findSegs: function(segs, startUIndex, endUIndex) {
				var lIndex = binarySearch(segs, 
						function(seg) { return seg.endUIndex >= startUIndex; }
				);
				var rIndex = binarySearch(segs, 
						function(seg) { return seg.startUIndex >= endUIndex; }
				);
				if(rIndex == -1) {
					rIndex = segs.length;
				}
				if(lIndex == -1) {
					return [];
				} else {
					return segs.slice(lIndex, rIndex);
				}
			},
			
			initFeatureAas: function(seqFeatAnalysis, featAnalysis) {
				return $scope.svgParams.findSegs(seqFeatAnalysis.aas, featAnalysis.startUIndex, featAnalysis.endUIndex);
			},
			
			initAaProps: function(featureAas, featAnalysis) {
				var params = $scope.svgParams;
				return _.map(featureAas, function(aa) {
	        		var nts = (aa.endUIndex - aa.startUIndex) + 1;
	    			var aaWidth = (nts * params.ntWidth) + ( (nts-1) * params.ntGap );
	    			var aaHeight = params.aaHeight;
		    		return {
		    			x: (aa.startUIndex - featAnalysis.startUIndex) * (params.ntWidth + params.ntGap),
		    			width: aaWidth,
		    			height: aaHeight,
		    			dx: aaWidth / 2.0,
		    			dy: aaHeight / 2.0,
		    			text: aa.aa
		    		};
		    	});
			},

			initFeatureNtSegs: function(seqFeatAnalysis, featAnalysis) {
				return $scope.svgParams.findSegs(seqFeatAnalysis.nts, featAnalysis.startUIndex, featAnalysis.endUIndex);
			},

			initNtSegProps: function(featureNtSegs, featAnalysis) {
				var params = $scope.svgParams;
				
				return _.map(featureNtSegs, function(featureNtSeg) {
					var truncateLeft = 0;
					if(featureNtSeg.startUIndex < featAnalysis.startUIndex) {
						truncateLeft = featAnalysis.startUIndex - featureNtSeg.startUIndex;
					}
					var truncateRight = 0;
					if(featureNtSeg.endUIndex > featAnalysis.endUIndex) {
						truncateRight = (featureNtSeg.endUIndex - featAnalysis.endUIndex);
					}
					var truncatedNts = featureNtSeg.nts.slice(truncateLeft, featureNtSeg.nts.length - truncateRight);
					
		    		var ntProps = _.map(truncatedNts, function(nt) {
		    			var ntWidth = params.ntWidth;
		    			var ntHeight = params.ntHeight;
	    				return {
			    			width: ntWidth,
			    			height: ntHeight,
			    			dx: ntWidth / 2.0,
			    			dy: ntHeight / 2.0,
	    					text: nt 
	    				};
	    			});
		    		for(var i = 0; i < ntProps.length; i++) {
		    			ntProps[i].x = ( (featureNtSeg.startUIndex + truncateLeft + i) - featAnalysis.startUIndex) * 
		    				(params.ntWidth + params.ntGap);
		    		}
		    		return {
		    			truncateLeft: truncateLeft,
		    			truncateRight: truncateRight,
		    			ntProps: ntProps, 
		    			indexDx: params.ntWidth / 2.0,
		    			indexDy: params.ntIndexHeight / 10.0,
		    			startIndexText: featureNtSeg.startSeqIndex + truncateLeft, 
		    			startIndexX: ( ( featureNtSeg.startUIndex + truncateLeft) - featAnalysis.startUIndex) * 
		    				(params.ntWidth + params.ntGap),
		    			endIndexText: featureNtSeg.endSeqIndex - truncateRight, 
		    			endIndexX: ( ( featureNtSeg.endUIndex - truncateRight ) - featAnalysis.startUIndex) * 
		    				(params.ntWidth + params.ntGap)
		    		}
				});
			},

			
			initVarProps: function(seqFeatAnalysis, featAnalysis) {
				var params = $scope.svgParams;
				var varProps = [];
				params.numVarTracks = 0;

				if(seqFeatAnalysis.variationMatchGroupPresent) {
					for(var i = 0; i < seqFeatAnalysis.variationMatchGroupPresent.length; i++) {
						var varMatchGroup = seqFeatAnalysis.variationMatchGroupPresent[i];
						for(var j = 0; j < varMatchGroup.variationMatch.length; j++) {
							var varMatch = varMatchGroup.variationMatch[j];
							if(varMatch.maxEndUIndex < featAnalysis.startUIndex) {
								continue;
							}
							if(varMatch.minStartUIndex > featAnalysis.endUIndex) {
								continue;
							}
							

			    			var highlightHeight = 
			    				params.aaHeight + 
			    				params.ntHeight + 
			    				params.ntIndexHeight + 
			    				( varMatch.track * (params.varHeight + params.varGap) );

			    			var propLocations = [];
			    			var propY = varMatch.track * (params.varHeight + params.varGap);
							for(var k = 0; k < varMatch.locations.length; k++) {
								var varMatchLocation = varMatch.locations[k];
				        		var nts = (varMatchLocation.endUIndex - varMatchLocation.startUIndex) + 1;
				    			var locWidth = (nts * params.ntWidth) + ( (nts-1) * params.ntGap );
				    			var locHeight = params.varHeight;
				    			var propLocation = {
					    			x: (varMatchLocation.startUIndex - featAnalysis.startUIndex) * (params.ntWidth + params.ntGap),
					    			y: propY,
					    			width: locWidth,
					    			height: locHeight,
					    			dx: locWidth / 2.0,
					    			dy: locHeight / 2.0,
				    			};
				    			propLocations.push(propLocation);
							}
							var pLocConnector = null;
			    			if(propLocations.length > 1) {
			    				pLocConnector = {
			    					x: propLocations[0].x + propLocations[0].width,
			    					y: (propY + locHeight / 2.0) - (params.pLocConnectorHeight / 2.0),
			    					width: propLocations[propLocations.length-1].x - (propLocations[0].x + propLocations[0].width),
			    					height: params.pLocConnectorHeight
			    				};
			    			}
							var varProp = {
								id: varMatchGroup.referenceName+"_"+varMatchGroup.featureName+"_"+varMatch.variationName,
								varReferenceName: varMatchGroup.referenceName,
								varFeatureName: varMatchGroup.featureName,
								variationCategory: varMatchGroup.variationCategory,
								variationName: varMatch.variationName,
								propLocations: propLocations,
								text: varMatch.variationRenderedName,
								highlightHeight: highlightHeight,
								pLocConnector: pLocConnector
							};
							varProps.push(varProp);
							if(varMatch.track >= params.numVarTracks) {
								params.numVarTracks = varMatch.track + 1;
							}
						}
					}
				}
				return varProps;
			},

			svgVariations: function() {
				var params = $scope.svgParams;
				return params.numVarTracks != 0;
			},
			
			codonLabelLineY: function() {
				return 0;
			},
			codonLabelLineHeight: function() {
				return $scope.svgParams.codonLabelHeight;
			},
			sequenceY: function(sequenceIndex) {
				var params = $scope.svgParams;
				var result =  
					params.codonLabelLineY() + 
					params.codonLabelLineHeight() + 
					(sequenceIndex * params.sequenceHeight());
				return result;
			},
			sequenceHeight: function() {
				var params = $scope.svgParams;
				return params.aaHeight + params.ntHeight + params.ntIndexHeight;
			}
	};
	
	$scope.svgHeight = function() {
		if($scope.selectedQueryFeatAnalysis) {
			var params = $scope.svgParams;
			var height = 
				params.codonLabelLineY() + 
				params.codonLabelLineHeight() + 		// codon label 
				params.sequenceHeight()+				// reference
				params.sequenceHeight()+				// query
				params.numVarTracks * 
					(params.varHeight + params.varGap);	// variation tracks. 
			return height;
		} else {
			return 0;
		}
	};
	$scope.svgWidth = function() {
		if($scope.selectedFeatureAnalysis) {
			var nts = ($scope.selectedFeatureAnalysis.endUIndex - $scope.selectedFeatureAnalysis.startUIndex) + 1;
			return (nts * $scope.svgParams.ntWidth) + ( (nts-1) * $scope.svgParams.ntGap );
		} else {
			return 0;
		}
	};
	
	
}]);