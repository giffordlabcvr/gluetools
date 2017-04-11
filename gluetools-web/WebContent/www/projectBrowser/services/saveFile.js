projectBrowser.service("saveFile", ['dialogs', 'glueWebToolConfig', 'FileSaver', function(dialogs, glueWebToolConfig, FileSaver) {
	
	this.saveFile = function(blob, fileDesc, defaultFilename) {
		var dlg = dialogs.create(
				glueWebToolConfig.getProjectBrowserURL()+'/dialogs/selectSaveFile.html','selectSaveFileCtrl',
				{ 
					fileName: defaultFilename,
					fileDesc: fileDesc
				}, {});

		dlg.result.then(function(data){
		    FileSaver.saveAs(blob, data.fileName);
		});

		return;
	};
}]);