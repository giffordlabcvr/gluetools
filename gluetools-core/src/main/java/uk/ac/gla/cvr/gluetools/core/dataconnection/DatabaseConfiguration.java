package uk.ac.gla.cvr.gluetools.core.dataconnection;

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
