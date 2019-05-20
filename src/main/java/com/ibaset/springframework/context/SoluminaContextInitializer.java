package com.ibaset.springframework.context;

import static com.ibaset.common.FrameworkConstants.JAVAMELODY_PREFIX;
import static com.ibaset.common.FrameworkConstants.MONITORING_STORAGE_DIRECTORY;
import static com.ibaset.common.FrameworkConstants.SOLUMINA_CONFIG_FILE;
import static com.ibaset.common.FrameworkConstants.SOLUMINA_MONITORING_DIR;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.trimAllWhitespace;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Application context initializer to initialize spring context.
 * 
 * While initializing context, it looks for soluminaConfig.properties file. If
 * soluminaConfig.properties is available on classpath it will check for below
 * properties:
 * <ul>
 * <li>If it has spring.profiles.active property defined, then it overrides
 * activated spring profiles with spring.profiles.active values</li>
 * <li>If it has solumina.monitoring.dir property defined, then it will set the
 * solumina monitoring directory with solumina.monitoring.dir value otherwise it
 * will set the default initial monitoring directory</li>
 * </ul>
 * If soluminaConfig.properties file is not available on classpath then it will
 * set default initial monitoring directory value
 *
 */
public class SoluminaContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final Logger logger = LoggerFactory.getLogger(SoluminaContextInitializer.class);

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {

		ConfigurableEnvironment environment = applicationContext.getEnvironment();

		Properties properties = loadPropertiesFromSoluminaConfig();

		if (!properties.isEmpty()) {
			overrideSpringActiveProfiles(environment, properties);
			setMonitoringStorageDirectory(properties);
		} else {
			setDefaultSoluminaMonitoringStorageDir();
		}

	}

	private Properties loadPropertiesFromSoluminaConfig() {
		Properties properties = new Properties();
		try (InputStream soluminaConfig = getClass().getClassLoader().getResourceAsStream(SOLUMINA_CONFIG_FILE)) {

			if (soluminaConfig != null) {
				logger.info("Solumina configuration file(" + SOLUMINA_CONFIG_FILE + ") found on classpath!");
				properties.load(soluminaConfig);
			}

		} catch (IOException e) {
			logger.warn(
					"Solumina configuration file(" + SOLUMINA_CONFIG_FILE + ") not found on application classpath!");
		}
		return properties;
	}

	private void overrideSpringActiveProfiles(ConfigurableEnvironment environment, Properties properties) {
		String profiles = (String) properties.get(ACTIVE_PROFILES_PROPERTY_NAME);
		if (hasText(profiles)) {
			String[] springActiveProfiles = commaDelimitedListToStringArray(trimAllWhitespace(profiles));
			environment.setActiveProfiles(springActiveProfiles);
		}
	}

	private void setMonitoringStorageDirectory(Properties properties) {
		String monitoringStorageDir = (String) properties.get(SOLUMINA_MONITORING_DIR);
		if (hasText(monitoringStorageDir)) {
			monitoringStorageDir = FilenameUtils.separatorsToSystem(monitoringStorageDir);
			System.setProperty(JAVAMELODY_PREFIX + MONITORING_STORAGE_DIRECTORY, monitoringStorageDir);
			logger.info("Monitoring storage directory is set to: " + monitoringStorageDir);
		} else {
			setDefaultSoluminaMonitoringStorageDir();
		}

	}

	private void setDefaultSoluminaMonitoringStorageDir() {
		String storageDir = EMPTY;
		String catalinaBase = System.getProperty("catalina.base");
		String catalinaHome = System.getProperty("catalina.home");
		if (catalinaBase != null && isNotEmpty(catalinaBase))
			storageDir = catalinaBase + "/logs/monitoring";
		else if (catalinaHome != null && isNotEmpty(catalinaHome))
			storageDir = catalinaHome + "/logs/monitoring";
		else
			storageDir = System.getProperty("user.home") + "/SoluminaLogs/monitoring";
		storageDir = FilenameUtils.separatorsToSystem(storageDir);
		System.setProperty(JAVAMELODY_PREFIX + MONITORING_STORAGE_DIRECTORY, storageDir);
		logger.info("Monitoring storage directory is set to: " + storageDir);
	}
}
