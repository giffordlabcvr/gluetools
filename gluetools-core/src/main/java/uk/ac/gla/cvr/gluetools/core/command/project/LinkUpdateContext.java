package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link.Multiplicity;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.LinkException;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.LinkException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public class LinkUpdateContext {
	
	public enum UpdateType {
		SET(Multiplicity.ONE_TO_ONE, Multiplicity.MANY_TO_ONE) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				GlueDataObject currentValue = (GlueDataObject) thisObject.readProperty(linkUpdateContext.thisLinkName);
				if(currentValue == null || !currentValue.pkMap().equals(otherObject.pkMap())) {
					return true;
				}
				return false;
			}
			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				thisObject.setToOneTarget(linkUpdateContext.getThisLinkName(), otherObject, true);
			}
		}, 
		UNSET(Multiplicity.ONE_TO_ONE, Multiplicity.MANY_TO_ONE) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				GlueDataObject currentValue = (GlueDataObject) thisObject.readProperty(linkUpdateContext.thisLinkName);
				if(currentValue != null) {
					return true;
				}
				return false;
			}

			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				thisObject.setToOneTarget(linkUpdateContext.getThisLinkName(), null, true);
			}
		}, 
		CLEAR(Multiplicity.ONE_TO_MANY) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				List<?> currentValue = (List<?>) thisObject.readProperty(linkUpdateContext.thisLinkName);
				if(!currentValue.isEmpty()) {
					return true;
				}
				return false;
			}

			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				@SuppressWarnings("unchecked")
				List<GlueDataObject> currentValues = new ArrayList<GlueDataObject>((List<GlueDataObject>) thisObject.readProperty(linkUpdateContext.thisLinkName));
				for(GlueDataObject value: currentValues) {
					thisObject.removeToManyTarget(linkUpdateContext.getThisLinkName(), value, true);
				}
			}
		}, 
		ADD(Multiplicity.ONE_TO_MANY) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				GlueDataObject currentOtherValue = (GlueDataObject) otherObject.readProperty(linkUpdateContext.otherLinkName);
				if(currentOtherValue == null || !currentOtherValue.pkMap().equals(thisObject.pkMap())) {
					return true;
				}
				return false;
			}

			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				thisObject.addToManyTarget(linkUpdateContext.getThisLinkName(), otherObject, true);
			}
		}, 
		REMOVE(Multiplicity.ONE_TO_MANY) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				List<?> currentOtherValue = (List<?>) otherObject.readProperty(linkUpdateContext.otherLinkName);
				if(!currentOtherValue.isEmpty()) {
					return true;
				}
				return false;
			}

			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				thisObject.removeToManyTarget(linkUpdateContext.getThisLinkName(), otherObject, true);
			}
		};
		
		private EnumSet<Multiplicity> thisToOtherMults;
		
		UpdateType(Link.Multiplicity first, Link.Multiplicity ... rest) {
			this.thisToOtherMults = EnumSet.of(first, rest);
		}

		public EnumSet<Multiplicity> getThisToOtherMults() {
			return thisToOtherMults;
		}

		private String cmdName() {
			return name()+" link-target";
		}
		
		public void checkMultiplicity(LinkUpdateContext ctx) {
			if(!thisToOtherMults.contains(ctx.thisToOtherMultiplicity)) {
				UpdateType alt = null;
				switch(this) {
				case SET: alt = ADD; break;
				case UNSET: alt = REMOVE; break;
				case ADD: alt = SET; break;
				case REMOVE: alt = UNSET; break;
				case CLEAR: alt = UNSET; break;
				}
				throw new LinkException(Code.LINK_MULTIPLICITY_ERROR, ctx.thisTableName, ctx.thisLinkName, 
						"Cannot use '"+cmdName()+"' on link with multiplicity "+ctx.thisToOtherMultiplicity+
						": use '"+alt.cmdName()+"' instead");
			}
			
		}

		public abstract boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject);

		public abstract void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject);
	}
	
	private Link link;
	private boolean isSrcLink;
	private String thisTableName, otherTableName;
	private String thisLinkName, otherLinkName;
	private Link.Multiplicity thisToOtherMultiplicity;
	private Link.Multiplicity otherToThisMultiplicity;
	
	public LinkUpdateContext(Project project, String thisTableName, String linkName) {
		this.isSrcLink = false;
		this.thisTableName = thisTableName;
		this.link = project.getSrcTableLink(thisTableName, linkName);
		if(link != null) {
			this.isSrcLink = true;
		} else {
			this.link = project.getDestTableLink(thisTableName, linkName);
		}
		if(this.isSrcLink) {
			this.thisLinkName = this.link.getSrcLinkName();
			this.otherLinkName = this.link.getDestLinkName();
			this.otherTableName = this.link.getDestTableName();
			this.thisToOtherMultiplicity = Multiplicity.valueOf(this.link.getMultiplicity());
			this.otherToThisMultiplicity = this.thisToOtherMultiplicity.inverse();
		} else {
			this.thisLinkName = this.link.getDestLinkName();
			this.otherLinkName = this.link.getSrcLinkName();
			this.otherTableName = this.link.getSrcTableName();
			this.otherToThisMultiplicity = Multiplicity.valueOf(this.link.getMultiplicity());
			this.thisToOtherMultiplicity = this.otherToThisMultiplicity.inverse();
		}
	}
	public Link getLink() {
		return link;
	}
	public boolean isSrcLink() {
		return isSrcLink;
	}
	public String getThisTableName() {
		return thisTableName;
	}
	public String getOtherTableName() {
		return otherTableName;
	}
	public String getThisLinkName() {
		return thisLinkName;
	}
	public String getOtherLinkName() {
		return otherLinkName;
	}
	public Link.Multiplicity getThisToOtherMultiplicity() {
		return thisToOtherMultiplicity;
	}
	public Link.Multiplicity getOtherToThisMultiplicity() {
		return otherToThisMultiplicity;
	}
}