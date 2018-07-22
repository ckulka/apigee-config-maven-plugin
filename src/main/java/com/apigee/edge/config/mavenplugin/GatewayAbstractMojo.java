/**
 * Copyright (C) 2016 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apigee.edge.config.mavenplugin;

import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apigee.edge.config.utils.ServerProfile;
import com.apigee.edge.config.utils.ConfigReader;
import com.apigee.edge.config.utils.ConsolidatedConfigReader;
import org.apache.maven.plugin.AbstractMojo;

import org.apache.maven.plugin.MojoExecutionException;

abstract class GatewayAbstractMojo extends AbstractMojo {

	/**
	 * Directory containing the build files.
	 *
	 * @parameter property="project.build.directory"
	 */
	private File buildDirectory;
	
	/**
	 * Base directory of the project.
	 */
	@Parameter(property = "basedir")
	private File baseDirectory;

	/**
	 * Project Name
	 *
	 * @parameter property="project.name"
	 */
	private String projectName;
	
	/**
	 * Project version
	 *
	 * @parameter property="project.version"
	 */
	private String projectVersion;

	/**
	 * Project artifact id
	 * 
	 * @parameter property="project.artifactId"
	 */
	private String artifactId;
	
	/**
	 * Profile id
	 */
	@Parameter(property = "apigee.profile")
	private String id;
	

	/**
	 * Gateway host URL
	 * 
	 * @parameter property="apigee.hosturl"
	 */
	private String hostURL;
	

	/**
	 * Gateway env profile
	 * 
	 * @parameter property="apigee.env" default-value="${apigee.profile}"
	 */
	private String deploymentEnv;
	
	/**
	 * Gateway api version
	 * 
	 * @parameter property="apigee.apiversion"
	 */
	private String apiVersion;
	
	
	/**
	 * Gateway org name
	 * 
	 * @parameter property="apigee.org"
	 */
	private String orgName;
	
	/**
	 * Gateway host username
	 * 
	 * @parameter property="apigee.username"
	 */
	private String userName;
	
	/**
	 * Gateway host password
	 * 
	 * @parameter property="apigee.password"
	 */
	private String password;

	/**
	 * Build option
	 */
	@Parameter(property = "build.option")
	private String buildOption;
	
	
	/**
	 * Gateway options
	 */
	@Parameter(property = "apigee.config.options")
	private String options;

    /**
	 * Config dir
 	 */
    @Parameter(property = "apigee.config.dir")
	private String configDir;
	
	/**
	 * Export dir for Apigee Dev App Keys
	 */
	@Parameter(property = "apigee.config.exportDir")
	private String exportDir;
	
	/**
	 * Mgmt API OAuth token endpoint
	 */
	@Parameter(property = "apigee.tokenurl", defaultValue = "https://login.apigee.com/oauth/token")
	private String tokenURL;

	/**
	 * Mgmt API OAuth MFA - TOTP
	 */
	@Parameter(property = "apigee.mfatoken")
	private String mfaToken;

	/**
	 * Mgmt API authn type
	 */
	@Parameter(property = "apigee.authtype", defaultValue = "basic")
	private String authType;
	
	/**
	 * Gateway bearer token
	 */
	@Parameter(property = "apigee.bearer")
	private String bearer;
	
	/**
	 * Gateway refresh token
	 */
	@Parameter(property = "apigee.refresh")
	private String refresh;
	
	/**
	 * Gateway OAuth clientId
	 */
	@Parameter(property = "apigee.clientid")
	private String clientid;
	
	/**
	 * Gateway OAuth clientSecret
	 */
	@Parameter(property = "apigee.clientsecret")
	private String clientsecret;
	
	// TODO set resources/edge as default value

	String getExportDir() {
		return exportDir;
	}

	/**
	* Skip running this plugin. Default is false.
	*/
	@Parameter(property = "skip", defaultValue = "false")
	private boolean skip = false;

	private ServerProfile buildProfile;

	public GatewayAbstractMojo(){
		super();
		
	}

	ServerProfile getProfile() {
		this.buildProfile = new ServerProfile();
		this.buildProfile.setOrg(this.orgName);
		this.buildProfile.setApplication(this.projectName);
		this.buildProfile.setApi_version(this.apiVersion);
		this.buildProfile.setHostUrl(this.hostURL);
		this.buildProfile.setEnvironment(this.deploymentEnv);
		this.buildProfile.setCredential_user(this.userName);
		this.buildProfile.setCredential_pwd(this.password);
		this.buildProfile.setProfileId(this.id);
		this.buildProfile.setOptions(this.options);
		this.buildProfile.setTokenUrl(this.tokenURL);
		this.buildProfile.setMFAToken(this.mfaToken);
		this.buildProfile.setAuthType(this.authType);
		this.buildProfile.setBearerToken(this.bearer);
		this.buildProfile.setRefreshToken(this.refresh);
		this.buildProfile.setClientId(this.clientid);
		this.buildProfile.setClientSecret(this.clientsecret);
		return buildProfile;
	}

	public void setProfile(ServerProfile profile) {
		this.buildProfile = profile;
	}

    void setConfigDir(String configDir) {
        this.configDir = configDir;
    }

	void setBaseDirectory(File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	String getBaseDirectoryPath(){
		return this.baseDirectory.getAbsolutePath();
	}

	String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}


	boolean isSkip() {
		return skip;
	}

	File findConsolidatedConfigFile() {
		File configFile = new File(getBaseDirectoryPath() + File.separator +
									"edge.json");
		if (configFile.exists()) {
			return configFile;
		}

		File yamlFile = Paths.get(getBaseDirectoryPath(), "edge.yaml").toFile();
		if (yamlFile.isFile()) {
			return yamlFile;
		}

		return null;
	}

	File findConfigFile(String scope, String config) {
		File configFile = new File(configDir + File.separator +
									scope + File.separator +
									config + ".json");
		if (configFile.exists()) {
			return configFile;
		}

		File yamlFile = Paths.get(configDir, scope, config + ".yaml").toFile();
		if (yamlFile.isFile()) {
			return yamlFile;
		}

		return null;
	}

	List getAPIConfig(Logger logger, String config, String api)
			throws MojoExecutionException {
		File configFile;
		String scope = "api" + File.separator + api;

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			configFile = findConfigFile(scope, config);
			if (configFile == null) {
				logger.info("Config file " + scope + File.separator + config + ".(json|yaml) not found.");
				return null;
			}

			logger.info("Retrieving config from " + configFile.getName());
			try {
				return ConfigReader.getAPIConfig(configFile);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.(json|yaml) not found");
		}

		logger.debug("Retrieving config from edge.(json|yaml)");
		try {

			return ConsolidatedConfigReader.getAPIConfig(configFile,
					api,
					config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	Set<String> getAPIList(Logger logger)
			throws MojoExecutionException {
		File configFile;
		String scope = configDir + File.separator + "api";

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			logger.info("Retrieving API list from " + scope);
			try {
				return ConfigReader.getAPIList(scope);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.json not found");
		}

		logger.debug("Retrieving config from edge.json");
		try {
			return ConsolidatedConfigReader.getAPIList(configFile);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	/*
	*  env picked from maven profile
	*  No support for maven profile names itself */
	List getEnvConfig(Logger logger, String config)
			throws MojoExecutionException {
		File configFile;
		String scope = "env" + File.separator + this.buildProfile.getEnvironment();

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			configFile = findConfigFile(scope, config);
			if (configFile == null) {
				logger.info("Config file " + scope + File.separator + config + ".(json|yaml) not found.");
				return null;
			}

			logger.info("Retrieving config from " + configFile.getName());
			try {
				return ConfigReader.getEnvConfig(this.buildProfile.getEnvironment(),
													configFile);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.json not found");
		}

		logger.debug("Retrieving config from edge.json");
		try {
			return ConsolidatedConfigReader.getEnvConfig(
					this.buildProfile.getEnvironment(),
							configFile,
							"envConfig",
							config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	List getOrgConfig(Logger logger, String config)
			throws MojoExecutionException {
		File configFile;
		String scope = "org";

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			configFile = findConfigFile(scope, config);
			if (configFile == null) {
				logger.info("Config file " + scope + File.separator + config + ".(json|yaml) not found.");
				return null;
			}

			logger.info("Retrieving config from " + configFile.getName());
			try {
				return ConfigReader.getOrgConfig(configFile);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.json not found");
		}

		logger.debug("Retrieving config from edge.json");
		try {
			return ConsolidatedConfigReader.getOrgConfig(configFile,
															"orgConfig",
															config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	Map getOrgConfigWithId(Logger logger, String config)
			throws MojoExecutionException {
		File configFile;
		String scope = "org";

		/* configDir takes precedence over edge.json */
		if (configDir != null && configDir.length() > 0) {
			configFile = findConfigFile(scope, config);
			if (configFile == null) {
				logger.info("Config file " + scope + File.separator + config + ".(json|yaml) not found.");
				return null;
			}

			logger.info("Retrieving config from " + configFile.getName());
			try {
				return ConfigReader.getOrgConfigWithId(configFile);
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
		}

		/* consolidated edge.json in CWD as fallback */
		configFile = findConsolidatedConfigFile();

		if (configFile == null) {
			logger.info("No edge.json found.");
			throw new MojoExecutionException("config file edge.json not found");
		}

		logger.debug("Retrieving config from edge.json");
		try {
			return ConsolidatedConfigReader.getOrgConfigWithId(configFile,
					"orgConfig",
					config);
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}
}
