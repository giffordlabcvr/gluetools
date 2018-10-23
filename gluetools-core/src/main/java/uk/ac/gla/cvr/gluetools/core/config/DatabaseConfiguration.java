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
package uk.ac.gla.cvr.gluetools.core.config;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngineException;
import uk.ac.gla.cvr.gluetools.core.GluetoolsEngineException.Code;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class DatabaseConfiguration implements Plugin {

	public static final String DEFAULT_MYSQL_JDBC_URL = "jdbc:mysql://localhost:3306/GLUE_TOOLS?characterEncoding=UTF-8";
	public static final String DEFAULT_JDBC_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

	public enum Vendor {
		ApacheDerby("org.apache.derby.jdbc.EmbeddedDriver"),
		MySQL("com.mysql.jdbc.Driver");
		
		private String jdbcDriverClass;
		private Vendor(String jdbcDriverClass) {
			this.jdbcDriverClass = jdbcDriverClass;
		}
		
		public String getJdbcDriverClass() {
			return jdbcDriverClass;
		}
	}
	
	private Optional<String> username = Optional.empty();
	private Optional<String> password = Optional.empty();
	private String jdbcDriverClass;
	private String jdbcUrl;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		this.username = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "username", username.orElse(null)));
		this.password = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "password", password.orElse(null)));
		String vendor = PluginUtils.configureStringProperty(configElem, "vendor", false);
		if(vendor != null) {
			GlueLogger.getGlueLogger().warning("The 'vendor' config element is deprecated as of GLUE 1.1.6 or later.");
			if(vendor.equals("MySQL")) {
				GlueLogger.getGlueLogger().warning("You can safely remove the <vendor> element MySQL as of GLUE 1.1.6 or later");
			} else {
				GlueLogger.getGlueLogger().severe("The only supported DB vendor at present is MySQL");
				throw new GluetoolsEngineException(Code.DB_CONNECTION_ERROR, "The only supported DB vendor at present is MySQL");
			}
		}
		this.jdbcUrl = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "jdbcUrl", false)).orElse(DEFAULT_MYSQL_JDBC_URL);
		this.jdbcDriverClass = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "driverClass", false)).orElse(DEFAULT_JDBC_DRIVER_CLASS);
	}

	public Optional<String> getUsername() {
		return username;
	}

	public Optional<String> getPassword() {
		return password;
	}

	public String getDriverClass() {
		return jdbcDriverClass;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

}
