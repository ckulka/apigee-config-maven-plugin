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

/**                                                                                                                                     ¡¡
 * Goal to attach flow hooks in Apigee EDGE
 * scope: env
 *
 * @author saisaran.vaidyanathan
 */
@Mojo(name = "flowhooks", defaultPhase = LifecyclePhase.INSTALL)
public class FlowHookMojo extends GatewayAbstractMojo {
    private static Logger logger = LoggerFactory.getLogger(FlowHookMojo.class);
    private static final String ____ATTENTION_MARKER____ =
            "************************************************************************";

    enum OPTIONS {
        none, create, update, delete, sync
    }

    private OPTIONS buildOption = OPTIONS.none;

    private ServerProfile serverProfile;

    static class FlowHook {
        @Key
        String name;
    }

    private void init() {
        try {
            logger.info(____ATTENTION_MARKER____);
            logger.info("Apigee Flow Hook");
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

    private String getFlowhookName(String payload) throws MojoFailureException {
        Gson gson = new Gson();
        try {
            FlowHook flowHook = gson.fromJson(payload, FlowHook.class);
            return flowHook.name;
        } catch (JsonParseException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    /**
     * FlowHooks
     */
    private void doUpdate(List<String> flowhooks) throws MojoFailureException {
        try {
            //List existingFlowhooks = null;
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            for (String flowhook : flowhooks) {
                String flowhookName = getFlowhookName(flowhook);
                if (flowhookName == null) {
                    throw new IllegalArgumentException(
                            "Flowhook does not have a name.\n" + flowhook + "\n");
                }
                switch (buildOption) {
                    case update:
                        logger.info("Updating Flowhook " + flowhookName);
                        createUpdateFlowhook(serverProfile, flowhookName, flowhook, "Update");
                        break;
                    case create:
                        logger.info("Attaching Flowhook " + flowhookName);
                        createUpdateFlowhook(serverProfile, flowhookName, flowhook, "Create");
                        break;
                    case delete:
                        logger.info("Detaching Flowhook " + flowhookName);
                        deleteFlowhook(serverProfile, flowhookName, flowhook);
                        break;
                    case sync:
                        logger.info("Detaching Flowhook " + flowhookName);
                        deleteFlowhook(serverProfile, flowhookName, flowhook);
                        logger.info("Attaching Flowhook " + flowhookName);
                        createUpdateFlowhook(serverProfile, flowhookName, flowhook, "Create");
                        break;
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

        Logger logger = LoggerFactory.getLogger(FlowHookMojo.class);

        init();

        if (buildOption == OPTIONS.none) {
            logger.info("Skipping Flow Hooks (default action)");
            return;
        }

        if (serverProfile.getEnvironment() == null) {
            throw new MojoExecutionException(
                    "Apigee environment not found in profile");
        }

        List flowhooks = getEnvConfig(logger, "flowhooks");
        if (flowhooks == null || flowhooks.size() == 0) {
            logger.info("No flowhooks config found.");
            return;
        }

        doUpdate(flowhooks);
    }

    /***************************************************************************
     * REST call wrappers
     **/

    private static void createUpdateFlowhook(ServerProfile profile, String flowhookName, String flowhook, String operation)
            throws IOException {

        HttpResponse response = RestUtil.updateEnvConfig(profile, "flowhooks", flowhookName, flowhook);
        try {

            logger.info("Response " + response.getContentType() + "\n" + response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info(operation + " Success.");

        } catch (HttpResponseException e) {
            logger.error("flowhook " + operation + " error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void deleteFlowhook(ServerProfile profile,
                                       String flowhookName,
                                       String flowhook)
            throws IOException {

        HttpResponse response = RestUtil.deleteEnvConfig(profile, "flowhooks",
                flowhookName, flowhook);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("Flowhook delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    public static List getFlowhook(ServerProfile profile)
            throws IOException {

        HttpResponse response = RestUtil.getEnvConfig(profile, "flowhooks");
        if (response == null) return new ArrayList();
        JSONArray flowhooks = null;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"flowhooks\": " + payload + "}";

            JSONParser parser = new JSONParser();
            JSONObject obj1 = (JSONObject) parser.parse(obj);
            flowhooks = (JSONArray) obj1.get("flowhooks");

        } catch (ParseException pe) {
            logger.error("Get flowhook parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get flowhook error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return flowhooks;
    }
}




