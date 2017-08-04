package uk.ac.gla.cvr.gluetools.ws;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager;

@WebServlet("/glue_web_files/*")
public class GlueWebFilesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String filename = URLDecoder.decode(request.getPathInfo().substring(1), "UTF-8");
        WebFilesManager webFilesManager = GluetoolsEngine.getInstance().getWebFilesManager();
		Path webFilesRootDir = webFilesManager.getWebFilesRootDir();
		Path requestedFile = webFilesRootDir.resolve(filename);
        response.setHeader("Content-Type", "application/octet-stream"); // force download
        response.setHeader("Content-Length", Long.toString(Files.size(requestedFile)));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + requestedFile.getFileName() + "\"");
        Files.copy(requestedFile, response.getOutputStream());
    }

}