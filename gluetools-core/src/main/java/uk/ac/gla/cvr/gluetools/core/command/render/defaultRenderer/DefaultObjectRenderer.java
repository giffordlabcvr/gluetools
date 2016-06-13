package uk.ac.gla.cvr.gluetools.core.command.render.defaultRenderer;

import java.io.IOException;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectException;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.IObjectRenderer;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import uk.ac.gla.cvr.gluetools.utils.Multiton;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

public class DefaultObjectRenderer implements IObjectRenderer {

	private static Multiton multiton = new Multiton();
	
	private Template template;
	
	public static DefaultObjectRenderer getDefaultObjectRenderer(Class<? extends GlueDataObject> dataObjectClass) {
		return multiton.get(new Key(dataObjectClass));
	}

	
	private DefaultObjectRenderer(Class<? extends GlueDataObject> dataObjectClass) {
		String ftlFile = dataObjectClass.getAnnotation(GlueDataClass.class).defaultObjectRendererFtlFile();
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
		TemplateModel templateModel = FreemarkerUtils.templateModelForGlueDataObject(renderableObject);
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
