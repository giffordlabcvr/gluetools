analysisTool.controller('analysisSvg', ['$scope', function($scope) {
	$scope.svgParams = {
			sequenceLabelWidth: 150,
			ntWidth: 16,
			ntHeight: 16,
			ntIndexHeight: 55,
			ntGap: 4,
			codonLabelHeight: 35,
			aaHeight: 25,
			
			initAaProps: function(seqFeatAnalysis, featAnalysis) {
				var params = $scope.svgParams;
				return _.map(seqFeatAnalysis.aas, function(aa) {
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
			initNtSegProps: function(seqFeatAnalysis, featAnalysis) {
				var params = $scope.svgParams;
				return _.map(seqFeatAnalysis.nts, function(queryNtSeg) {
		    		var ntProps = _.map(queryNtSeg.nts, function(nt) {
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
		    			ntProps[i].x = ( (queryNtSeg.startUIndex + i) - featAnalysis.startUIndex) * 
		    				(params.ntWidth + params.ntGap);
		    		}
		    		return {
		    			ntProps: ntProps, 
		    			indexDx: params.ntWidth / 2.0,
		    			indexDy: params.ntIndexHeight / 10.0,
		    			startIndexText: queryNtSeg.startSeqIndex, 
		    			startIndexX: (queryNtSeg.startUIndex - featAnalysis.startUIndex) * (params.ntWidth + params.ntGap),
		    			endIndexText: queryNtSeg.endSeqIndex, 
		    			endIndexX: (queryNtSeg.endUIndex - featAnalysis.startUIndex) * (params.ntWidth + params.ntGap)
		    			
		    		}
				});
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
		var params = $scope.svgParams;
		var height = 
			params.codonLabelLineY() + 
			params.codonLabelLineHeight() + // codon label 
			params.sequenceHeight()+	// reference
			params.sequenceHeight();	// query
		return height;
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