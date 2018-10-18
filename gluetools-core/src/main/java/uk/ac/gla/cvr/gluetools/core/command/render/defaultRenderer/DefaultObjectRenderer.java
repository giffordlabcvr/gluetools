/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.render.defaultRenderer;

import java.io.IOException;

import freemarker.template.Template;
import freemarker.template.TemplateModel;
import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectException;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.IObjectRenderer;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class DefaultObjectRenderer implements IObjectRenderer {

	private static Multiton multiton = new Multiton();
	
	private Template template;
	
	public static DefaultObjectRenderer getDefaultObjectRenderer(Class<? extends GlueDataObject> dataObjectClass) {
		return multiton.get(new Key(dataObjectClass));
	}

	
	private DefaultObjectRenderer(Class<? extends GlueDataObject> dataObjectClass) {
		String ftlFile = Project.getDataClassAnnotation(dataObjectClass).defaultObjectRendererFtlFile();
		if(ftlFile == null || ftlFile.length() == 0) {
			throw new RenderObjectException(Code.NO_DEFAULT_RENDERER_DEFINED, dataObjectClass.getName());
		}
		try {
			this.template = GluetoolsEngine.getInstance().getFreemarkerConfiguration().getTemplate(ftlFile);
		} catch(IOException e) {
			throw new RenderObjectException(e, Code.ERROR_LOADING_DEFAULT_RENDERER_TEMPLATE, ftlFile, e.getLocalizedMessage());
		}
	}
	
	
	@Override
	public DocumentResult render(CommandContext cmdContext, GlueDataObject renderableObject) {
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(renderableObject);
		return IObjectRenderer.documentResultFromBytes(FreemarkerUtils.processTemplate(template, templateModel).getBytes());
	}
	
	private static class Key implements Multiton.Creator<DefaultObjectRenderer> {
		private String dataObjectClassName;
		private Class<? extends GlueDataObject> dataObjectClass;
		
		public Key(Class<? extends GlueDataObject> dataObjectClass) {
			super();
			this.dataObjectClassName = dataObjectClass.getName();
			this.dataObjectClass = dataObjectClass;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((dataObjectClassName == null) ? 0 : dataObjectClassName
							.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (dataObjectClassName == null) {
				if (other.dataObjectClassName != null)
					return false;
			} else if (!dataObjectClassName.equals(other.dataObjectClassName))
				return false;
			return true;
		}

		@Override
		public Object create() {
			return new DefaultObjectRenderer(dataObjectClass);
		}

		@Override
		public DefaultObjectRenderer cast(Object value) {
			return (DefaultObjectRenderer) value;
		}
	}

	
}
