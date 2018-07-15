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

import com.apigee.edge.config.mavenplugin.kvm.*;
import com.apigee.edge.config.rest.RestUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.apache.maven.plugin.MojoExecutionException;
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

/**
 * Goal to create KVM in Apigee EDGE.
 * scope: org, env, api
 *
 * @author madhan.sadasivam
 */
@Mojo(name = "kvms", defaultPhase = LifecyclePhase.INSTALL)
public class KVMMojo extends GatewayAbstractMojo {
    private static Logger logger = LoggerFactory.getLogger(KVMMojo.class);
    private static final String ____ATTENTION_MARKER____ =
            "************************************************************************";

    enum OPTIONS {
        none, create, update, delete, sync
    }

    private OPTIONS buildOption = OPTIONS.none;

    private ServerProfile serverProfile;
    private Kvm kvmOrg;
    private Kvm kvmApi;
    private Kvm kvmEnv;

    static class KVM {
        @Key
        String name;
    }

    KVMMojo() {
        super();

    }

    private void init() {

        try {
            logger.info(____ATTENTION_MARKER____);
            logger.info("Apigee KVM");
            logger.info(____ATTENTION_MARKER____);

            serverProfile = super.getProfile();
            kvmOrg = new KvmOrg();
            kvmApi = new KvmApi();
            kvmEnv = new KvmEnv();

            String options = super.getOptions();
            if (options != null) {
                buildOption = OPTIONS.valueOf(options);
            }
            logger.debug("Build option " + buildOption.name());
            logger.debug("Base dir " + super.getBaseDirectoryPath());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid apigee.option provided");
        }

    }

    private String getKVMName(String payload) throws MojoExecutionException {
        try {
            return new Gson().fromJson(payload, KVM.class).name;
        } catch (JsonParseException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private void doOrgUpdate(List<String> kvms, String scope) throws MojoExecutionException {
        try {
            List existingKVM = getOrgKVM(serverProfile);
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            for (String kvm : kvms) {
                String kvmName = getKVMName(kvm);
                if (kvmName == null) {
                    throw new IllegalArgumentException(
                            "KVM does not have a name.\n" + kvm + "\n");
                }

                if (existingKVM.contains(kvmName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Org KVM \"" + kvmName +
                                    "\" exists. Updating.");
                            kvmOrg.update(new KvmValueObject(serverProfile, kvmName, kvm));
                            break;
                        case create:
                            logger.info("Org KVM \"" + kvmName +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Org KVM \"" + kvmName +
                                    "\" already exists. Deleting.");
                            deleteOrgKVM(serverProfile, kvmName);
                            break;
                        case sync:
                            logger.info("Org KVM \"" + kvmName +
                                    "\" already exists. Deleting and recreating.");
                            deleteOrgKVM(serverProfile, kvmName);
                            logger.info("Creating Org KVM - " + kvmName);
                            createOrgKVM(serverProfile, kvm);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Org KVM - " + kvmName);
                            createOrgKVM(serverProfile, kvm);
                            break;
                        case delete:
                            logger.info("Org KVM \"" + kvmName +
                                    "\" does not exist. Skipping.");
                            break;
                    }
                }
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Apigee network call error " + e.getMessage());
        }
    }

    private void doEnvUpdate(List<String> kvms, String scope) throws MojoExecutionException {
        try {
            List existingKVM = getEnvKVM(serverProfile);
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            for (String kvm : kvms) {
                String kvmName = getKVMName(kvm);
                if (kvmName == null) {
                    throw new IllegalArgumentException(
                            "KVM does not have a name.\n" + kvm + "\n");
                }

                if (existingKVM.contains(kvmName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("Env KVM \"" + kvmName +
                                    "\" exists. Updating.");
                            kvmEnv.update(new KvmValueObject(serverProfile, kvmName, kvm));
                            break;
                        case create:
                            logger.info("Env KVM \"" + kvmName +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("Env KVM \"" + kvmName +
                                    "\" already exists. Deleting.");
                            deleteEnvKVM(serverProfile, kvmName);
                            break;
                        case sync:
                            logger.info("Env KVM \"" + kvmName +
                                    "\" already exists. Deleting and recreating.");
                            deleteEnvKVM(serverProfile, kvmName);
                            logger.info("Creating Env KVM - " + kvmName);
                            createEnvKVM(serverProfile, kvm);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating Env KVM - " + kvmName);
                            createEnvKVM(serverProfile, kvm);
                            break;
                        case delete:
                            logger.info("Env KVM \"" + kvmName +
                                    "\" does not exist. Skipping.");
                            break;
                    }
                }
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Apigee network call error " + e.getMessage());
        }
    }

    private void doAPIUpdate(String api, List<String> kvms) throws MojoExecutionException {
        try {
            List existingKVM = getAPIKVM(serverProfile, api);
            if (buildOption != OPTIONS.update &&
                    buildOption != OPTIONS.create &&
                    buildOption != OPTIONS.delete &&
                    buildOption != OPTIONS.sync) {
                return;
            }

            for (String kvm : kvms) {
                String kvmName = getKVMName(kvm);
                if (kvmName == null) {
                    throw new IllegalArgumentException(
                            "KVM does not have a name.\n" + kvm + "\n");
                }

                if (existingKVM.contains(kvmName)) {
                    switch (buildOption) {
                        case update:
                            logger.info("API KVM \"" + kvmName +
                                    "\" exists. Updating.");
                            kvmApi.update(new KvmValueObject(serverProfile, api, kvmName, kvm));
                            break;
                        case create:
                            logger.info("API KVM \"" + kvmName +
                                    "\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("API KVM \"" + kvmName +
                                    "\" already exists. Deleting.");
                            deleteAPIKVM(serverProfile, api, kvmName);
                            break;
                        case sync:
                            logger.info("API KVM \"" + kvmName +
                                    "\" already exists. Deleting and recreating.");
                            deleteAPIKVM(serverProfile, api, kvmName);
                            logger.info("Creating API KVM - " + kvmName);
                            createAPIKVM(serverProfile, api, kvm);
                            break;
                    }
                } else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating API KVM - " + kvmName);
                            createAPIKVM(serverProfile, api, kvm);
                            break;
                        case delete:
                            logger.info("API KVM \"" + kvmName +
                                    "\" does not exist. Skipping.");
                            break;
                    }
                }
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Apigee network call error " + e.getMessage());
        }
    }

    /**
     * Entry point for the mojo.
     */
    public void execute() throws MojoExecutionException {

        if (super.isSkip()) {
            getLog().info("Skipping");
            return;
        }

        Logger logger = LoggerFactory.getLogger(KVMMojo.class);

        init();

        if (buildOption == OPTIONS.none) {
            logger.info("Skipping KVM (default action)");
            return;
        }

        if (serverProfile.getEnvironment() == null) {
            throw new MojoExecutionException(
                    "Apigee environment not found in profile");
        }

        /* org scoped KVMs */
        String scope = "orgConfig";
        List kvms = getOrgConfig(logger, "kvms");
        if (kvms == null || kvms.size() == 0) {
            logger.info("No org scoped KVM config found.");
        } else {
            doOrgUpdate(kvms, scope);
        }

        /* env scoped KVMs */
        kvms = getEnvConfig(logger, "kvms");
        if (kvms == null || kvms.size() == 0) {
            logger.info("No env scoped KVM config found.");
        } else {
            doEnvUpdate(kvms, scope);
        }

        // /* API scoped KVMs */
        Set<String> apis = getAPIList(logger);
        if (apis == null || apis.size() == 0) {
            logger.info("No API scoped KVM config found.");
            return;
        }

        for (String api : apis) {
            kvms = getAPIConfig(logger, "kvms", api);
            if (kvms == null || kvms.size() == 0) {
                logger.info(
                        "No API scoped KVM config found for " + api);
            } else {
                doAPIUpdate(api, kvms);
            }
        }

    }

    /***************************************************************************
     * REST call wrappers
     **/
    private static void createOrgKVM(ServerProfile profile, String kvm)
            throws IOException {

        HttpResponse response = RestUtil.createOrgConfig(profile,
                "keyvaluemaps",
                kvm);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void deleteOrgKVM(ServerProfile profile, String kvmEntry) throws IOException {

        HttpResponse response = RestUtil.deleteOrgConfig(profile,
                "keyvaluemaps",
                kvmEntry);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static List getOrgKVM(ServerProfile profile) throws IOException {

        HttpResponse response = RestUtil.getOrgConfig(profile, "keyvaluemaps");
        if (response == null) return new ArrayList();
        JSONArray kvms;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"kvms\": " + payload + "}";

            JSONParser parser = new JSONParser();
            JSONObject obj1 = (JSONObject) parser.parse(obj);
            kvms = (JSONArray) obj1.get("kvms");

        } catch (ParseException pe) {
            logger.error("Get KVM parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get KVM error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return kvms;
    }

    private static void createEnvKVM(ServerProfile profile, String kvm) throws IOException {

        HttpResponse response = RestUtil.createEnvConfig(profile,
                "keyvaluemaps",
                kvm);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void deleteEnvKVM(ServerProfile profile, String kvmEntry) throws IOException {

        HttpResponse response = RestUtil.deleteEnvConfig(profile,
                "keyvaluemaps",
                kvmEntry);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static List getEnvKVM(ServerProfile profile) throws IOException {

        HttpResponse response = RestUtil.getEnvConfig(profile, "keyvaluemaps");
        if (response == null) return new ArrayList();
        JSONArray kvms;
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"kvms\": " + payload + "}";

            JSONParser parser = new JSONParser();
            JSONObject obj1 = (JSONObject) parser.parse(obj);
            kvms = (JSONArray) obj1.get("kvms");

        } catch (ParseException pe) {
            logger.error("Get KVM parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get KVM error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return kvms;
    }

    private static void createAPIKVM(ServerProfile profile, String api, String kvm) throws IOException {

        HttpResponse response = RestUtil.createAPIConfig(profile,
                api,
                "keyvaluemaps",
                kvm);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static void deleteAPIKVM(ServerProfile profile,
                                     String api,
                                     String kvmEntry)
            throws IOException {

        HttpResponse response = RestUtil.deleteAPIConfig(profile,
                api,
                "keyvaluemaps",
                kvmEntry);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

    private static List getAPIKVM(ServerProfile profile, String api)
            throws IOException {

        HttpResponse response = RestUtil.getAPIConfig(profile, api,
                "keyvaluemaps");
        if (response == null) return new ArrayList();
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);

            /* Parsers fail to parse a string array.
             * converting it to an JSON object as a workaround */
            String obj = "{ \"kvms\": " + payload + "}";

            JSONParser parser = new JSONParser();
            JSONObject obj1 = (JSONObject) parser.parse(obj);
            return (JSONArray) obj1.get("kvms");

        } catch (ParseException pe) {
            logger.error("Get KVM parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get KVM error " + e.getMessage());
            throw new IOException(e.getMessage());
        }
    }
}
