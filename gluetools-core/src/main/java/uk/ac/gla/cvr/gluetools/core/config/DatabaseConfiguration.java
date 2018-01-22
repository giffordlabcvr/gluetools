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

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class DatabaseConfiguration implements Plugin {

	public static final String DEFAULT_APACHE_DERBY_JDBC_URL = "jdbc:derby:memory:testdb;create=true";

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
	private Vendor vendor = Vendor.ApacheDerby;
	private String jdbcUrl = DEFAULT_APACHE_DERBY_JDBC_URL;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		username = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "username", username.orElse(null)));
		password = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "password", password.orElse(null)));
		vendor = PluginUtils.configureEnumProperty(Vendor.class, configElem, "vendor", vendor);
		jdbcUrl = PluginUtils.configureStringProperty(configElem, "jdbcUrl", jdbcUrl);
	}

	public Optional<String> getUsername() {
		return username;
	}

	public Optional<String> getPassword() {
		return password;
	}

	public Vendor getVendor() {
		return vendor;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

}
