/**
 * Copyright (C) 2016 Apigee Corporation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apigee.edge.config.mavenplugin;

import com.apigee.edge.config.rest.RestUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**                                                                                                                                     ¡¡
 * Goal to create Resource Files in Apigee EDGE.
 * scope: org, env, api
 *
 * @author saisaran.vaidyanathan
 */
@Mojo(name = "resourcefiles", defaultPhase = LifecyclePhase.INSTALL)
public class ResourceFileMojo extends GatewayAbstractMojo {
    private static Logger logger = LoggerFactory.getLogger(ResourceFileMojo.class);
    private static final String ____ATTENTION_MARKER____ =
            "************************************************************************";

    enum OPTIONS {
        none, create, update, delete, sync
    }

    private OPTIONS buildOption = OPTIONS.none;

    private ServerProfile serverProfile;

    protected static class ResourceFile {
        @Key
        String name;
        @Key
        String type;
        @Key
        String file;
        @Key
        String revision;
    }

    private ResourceFileMojo() {
        super();

    }

    private void init() {

        try {
            logger.info(____ATTENTION_MARKER____);
            logger.info("Apigee Resource File");
            logger.info(____ATTENTION_MARKER____);

            String options = "";
            serverProfile = super.getProfile();

            options = super.getOptions();
            if (options != null) {
                buildOption = OPTIONS.valueOf(options);
            }
            logger.debug("Build option " + buildOption.name());
            logger.debug("Base dir " + super.getBaseDirectoryPath());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid apigee.option provided");
        }

    }

    private ResourceFile getResourceFile(String payload) throws MojoFailureException {
        Gson gson = new Gson();
        try {
            return gson.fromJson(payload, ResourceFile.class);
        } catch (JsonParseException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    private void doOrgUpdate(List<String> resourcefiles, String scope)
            throws MojoFailureException {
        try {
            List existingResourcefile = getExistingResourceFile(serverProfile, "org", null);
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            for (String resourcefile : resourcefiles) {
                String resourcefileName = getResourceFile(resourcefile).name;
                String resourcefileType = getResourceFile(resourcefile).type;
                String resourcefilePath = getResourceFile(resourcefile).file;
                if (resourcefileName == null) {
                    throw new IllegalArgumentException(
                            "Resource File does not have a name.\n" + resourcefile + "\n");
                }

                if (existingResourcefile.contains(resourcefileName + "_" + resourcefileType)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Org Resource File \"" + resourcefileName +
                                    "\" exists. Updating.");
                            updateOrgResourceFile(serverProfile, resourcefileType, resourcefileName, resourcefilePath);
                            break;
                        case create:
                            logger.info("Org Resource File \"" + resourcefileName + " of type " + resourcefileType +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Org Resource File \"" + resourcefileName + " of type " + resourcefileType +
                                    "\" already exists. Deleting.");
                            deleteOrgResourceFile(serverProfile, resourcefileType, resourcefileName);
                            break;
                        case sync:
                            logger.info("Org Resource File \"" + resourcefileName + " of type " + resourcefileType +
                                    "\" already exists. Deleting and recreating.");
                            deleteOrgResourceFile(serverProfile, resourcefileType, resourcefileName);
                            logger.info("Creating Org Resource File - " + resourcefileName);
                            createOrgResourceFile(serverProfile, resourcefileType, resourcefileName, resourcefilePath);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Org Resource File - " + resourcefileName + " of type " + resourcefileType);
                            createOrgResourceFile(serverProfile, resourcefileType, resourcefileName, resourcefilePath);
                            break;
                        case delete:
                            logger.info("Org Resource File \"" + resourcefileName + " of type " + resourcefileType +
                                    "\" does not exist. Skipping.");
                            break;
                    }
                }
            }

        } catch (IOException e) {
            throw new MojoFailureException("Apigee network call error " +
                    e.getMessage());
        }
    }

    private void doEnvUpdate(List<String> resourcefiles, String scope)
            throws MojoFailureException {
        try {
            List existingResourcefile = getExistingResourceFile(serverProfile, "env", null);
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            for (String resourcefile : resourcefiles) {
                String resourcefileName = getResourceFile(resourcefile).name;
                String resourcefileType = getResourceFile(resourcefile).type;
                String resourcefilePath = getResourceFile(resourcefile).file;
                if (resourcefileName == null) {
                    throw new IllegalArgumentException(
                            "resourcefile does not have a name.\n" + resourcefile + "\n");
                }

                if (existingResourcefile.contains(resourcefileName + "_" + resourcefileType)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Env Resource File \"" + resourcefileName +
                                    "\" exists. Updating.");
                            updateEnvResourceFile(serverProfile, resourcefileType, resourcefileName, resourcefilePath);
                            break;
                        case create:
                            logger.info("Env Resource File \"" + resourcefileName + " of type " + resourcefileType +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Env Resource File \"" + resourcefileName + " of type " + resourcefileType +
                                    "\" already exists. Deleting.");
                            deleteEnvResourceFile(serverProfile, resourcefileType, resourcefileName);
                            break;
                        case sync:
                            logger.info("Env Resource File \"" + resourcefileName + " of type " + resourcefileType +
                                    "\" already exists. Deleting and recreating.");
                            deleteEnvResourceFile(serverProfile, resourcefileType, resourcefileName);
                            logger.info("Creating Env Resource File - " + resourcefileName);
                            createEnvResourceFile(serverProfile, resourcefileType, resourcefileName, resourcefilePath);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Env Resource File - " + resourcefileName + " of type " + resourcefileType);
                            createEnvResourceFile(serverProfile, resourcefileType, resourcefileName, resourcefilePath);
                            break;
                        case delete:
                            logger.info("Env Resource File \"" + resourcefileName + " of type " + resourcefileType +
                                    "\" does not exist. Skipping.");
                            break;
                    }
                }
            }

        } catch (IOException e) {
            throw new MojoFailureException("Apigee network call error " +
                    e.getMessage());
        }
    }

    private void doAPIUpdate(String api, List<String> resourcefiles)
            throws MojoFailureException {
        try {
            //List existingResourcefile = getExistingResourceFile(serverProfile, "api", api);
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            for (String resourcefile : resourcefiles) {
                String resourcefileName = getResourceFile(resourcefile).name;
                String resourcefileType = getResourceFile(resourcefile).type;
                String resourcefilePath = getResourceFile(resourcefile).file;
                String revision = getResourceFile(resourcefile).revision;
                if (resourcefileName == null) {
                    throw new IllegalArgumentException(
                            "resourcefile does not have a name.\n" + resourcefile + "\n");
                }

                List existingResourcefile = getExistingResourceFile(serverProfile, "api", api + "/revisions/" + revision);

                if (existingResourcefile.contains(resourcefileName + "_" + resourcefileType)) {
                    switch (buildOption) {
                        case update:
                            logger.info("API Resource File \"" + resourcefileName +
                                    "\" exists. Updating.");
                            updateAPIResourceFile(serverProfile, api, revision, resourcefileType, resourcefileName, resourcefilePath);
                            break;
                        case create:
                            logger.info("API Resource File \"" + resourcefileName +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("API Resource File \"" + resourcefileName +
                                    "\" already exists. Deleting.");
                            deleteAPIResourceFile(serverProfile, api, revision, resourcefileType, resourcefileName);
                            break;
                        case sync:
                            logger.info("API Resource File \"" + resourcefileName +
                                    "\" already exists. Deleting and recreating.");
                            deleteAPIResourceFile(serverProfile, api, revision, resourcefileType, resourcefileName);
                            logger.info("Creating API Resource File - " + resourcefileName);
                            createAPIResourceFile(serverProfile, api, revision, resourcefileType, resourcefileName, resourcefilePath);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating API Resource File - " + resourcefileName);
                            createAPIResourceFile(serverProfile, api, revision, resourcefileType, resourcefileName, resourcefilePath);
                            break;
                        case delete:
                            logger.info("API Resource File \"" + resourcefileName +
                                    "\" does not exist. Skipping.");
                            break;
                    }
                }
            }

        } catch (IOException e) {
            throw new MojoFailureException("Apigee network call error " +
                    e.getMessage());
        }
    }

    /**
     * Entry point for the mojo.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (super.isSkip()) {
            getLog().info("Skipping");
            return;
        }

        Logger logger = LoggerFactory.getLogger(ResourceFileMojo.class);

        init();

        if (buildOption == OPTIONS.none) {
            logger.info("Skipping Resource File (default action)");
            return;
        }

        if (serverProfile.getEnvironment() == null) {
            throw new MojoExecutionException(
                    "Apigee environment not found in profile");
        }

        /* org scoped Resource Files */
        String scope = "orgConfig";
        List resourcefiles = getOrgConfig(logger, "resourcefiles");
        if (resourcefiles == null || resourcefiles.size() == 0) {
            logger.info("No org scoped resourcefiles config found.");
        } else {
            doOrgUpdate(resourcefiles, scope);
        }

        /* env scoped resourcefiles */
        resourcefiles = getEnvConfig(logger, "resourcefiles");
        if (resourcefiles == null || resourcefiles.size() == 0) {
            logger.info("No env scoped resourcefiles config found.");
        } else {
            doEnvUpdate(resourcefiles, scope);
        }

        // /* API scoped resourcefiles */
        Set<String> apis = getAPIList(logger);
        if (apis == null || apis.size() == 0) {
            logger.info("No API scoped Resource File config found.");
            return;
        }

        for (String api : apis) {
            resourcefiles = getAPIConfig(logger, "resourcefiles", api);
            if (resourcefiles == null || resourcefiles.size() == 0) {
                logger.info(
                        "No API scoped resourcefiles config found for " + api);
            } else {
                doAPIUpdate(api, resourcefiles);
            }
        }
    }

    /***************************************************************************
     * REST call wrappers
     **/
    private static void createOrgResourceFile(ServerProfile profile,
                                              String resourcefileType,
                                              String resourcefileName,
                                              String resourceFilePath)
            throws IOException {

        HttpResponse response = RestUtil.createOrgConfigUpload(profile,
                "resourcefiles" + "?type=" + resourcefileType + "&name=" + resourcefileName,
                resourceFilePath);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("resourcefile create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void updateOrgResourceFile(ServerProfile profile,
                                              String resourcefileType,
                                              String resourcefileName,
                                              String resourcefilePath)
            throws IOException {

        HttpResponse response = RestUtil.updateOrgConfigUpload(profile,
                "resourcefiles",
                resourcefileType + "/" + resourcefileName,
                resourcefilePath);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("resourceFile update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void deleteOrgResourceFile(ServerProfile profile, String resourcefileType, String resourcefileName)
            throws IOException {

        HttpResponse response = RestUtil.deleteOrgResourceFileConfig(profile,
                "resourcefiles",
                resourcefileType + "/" + resourcefileName);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("resourceFile delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void createEnvResourceFile(ServerProfile profile, String resourcefileType, String resourcefileName,
                                              String resourceFilePath) throws IOException {

        HttpResponse response = RestUtil.createEnvConfigUpload(profile,
                "resourcefiles" + "?type=" + resourcefileType + "&name=" + resourcefileName, resourceFilePath);
        try {

            logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("resourcefile create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void updateEnvResourceFile(ServerProfile profile, String resourcefileType, String resourcefileName,
                                              String resourcefilePath) throws IOException {

        HttpResponse response = RestUtil.updateEnvConfigUpload(profile, "resourcefiles",
                resourcefileType + "/" + resourcefileName, resourcefilePath);
        try {

            logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("resourceFile update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void deleteEnvResourceFile(ServerProfile profile, String resourcefileType, String resourcefileName)
            throws IOException {

        HttpResponse response = RestUtil.deleteEnvResourceFileConfig(profile, "resourcefiles",
                resourcefileType + "/" + resourcefileName);
        try {

            logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("resourceFile delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static List getExistingResourceFile(ServerProfile profile, String scope, String api) throws IOException {

        HttpResponse response = null;
        if (scope != null && scope.equalsIgnoreCase("org")) {
            response = RestUtil.getOrgConfig(profile, "resourcefiles");
        } else if (scope != null && scope.equalsIgnoreCase("env")) {
            response = RestUtil.getEnvConfig(profile, "resourcefiles");
        } else if (scope != null && scope.equalsIgnoreCase("api")) {
            response = RestUtil.getAPIConfig(profile, api, "resourcefiles");
        }
        if (response == null)
            return new ArrayList();
        JSONArray resourcefilesArr = null;
        List resourcefiles = new ArrayList();
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);
            JSONParser parser = new JSONParser();
            JSONObject obj1 = (JSONObject) parser.parse(payload);
            resourcefilesArr = (JSONArray) obj1.get("resourceFile");
            if (resourcefilesArr != null && resourcefilesArr.size() > 0) {
                for (int i = 0; i < resourcefilesArr.size(); i++) {
                    JSONObject resourcefile = (JSONObject) resourcefilesArr.get(i);
                    resourcefiles.add(resourcefile.get("name") + "_" + resourcefile.get("type"));
                }
            }
        } catch (ParseException pe) {
            logger.error("Get resourcefiles parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get resourcefiles error " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        return resourcefiles;
    }

    private static void createAPIResourceFile(ServerProfile serverProfile,
                                              String api, String revision,
                                              String resourcefileType, String resourcefileName,
                                              String resourcefilePath)
            throws IOException {
        HttpResponse response = RestUtil.createAPIConfigUpload(serverProfile,
                api,
                "revisions/" + revision + "/resourcefiles" + "?type=" + resourcefileType + "&name=" + resourcefileName,
                resourcefilePath);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("Resource File create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void updateAPIResourceFile(ServerProfile profile,
                                              String api, String revision,
                                              String resourcefileType, String resourcefileName,
                                              String resourcefilePath)
            throws IOException {

        HttpResponse response = RestUtil.updateAPIConfigUpload(profile,
                api,
                "revisions/" + revision + "/resourcefiles",
                resourcefileType + "/" + resourcefileName,
                resourcefilePath);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("Resource File update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void deleteAPIResourceFile(ServerProfile profile,
                                              String api, String revision,
                                              String resourcefileType, String resourcefileName)
            throws IOException {

        HttpResponse response = RestUtil.deleteAPIResourceFileConfig(profile,
                api,
                "revisions/" + revision + "/resourcefiles",
                resourcefileType + "/" + resourcefileName);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Resource File delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

}




