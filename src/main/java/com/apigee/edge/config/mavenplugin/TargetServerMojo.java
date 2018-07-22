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

/**
 * Goal to create target servers in Apigee EDGE.
 * scope: env
 *
 * @author madhan.sadasivam
 */
@Mojo(name = "targetservers", defaultPhase = LifecyclePhase.INSTALL)
public class TargetServerMojo extends GatewayAbstractMojo {
  private static Logger logger = LoggerFactory.getLogger(TargetServerMojo.class);
  private static final String ____ATTENTION_MARKER____ =
      "************************************************************************";

  enum OPTIONS {
    none, create, update, delete, sync
  }

  private OPTIONS buildOption = OPTIONS.none;

  private ServerProfile serverProfile;

  static class TargetServer {
    @Key
    String name;
  }

  private TargetServerMojo() {
    super();

  }

  private void init() {
    try {
      logger.info(____ATTENTION_MARKER____);
      logger.info("Apigee Target Servers");
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

  private String getTargetName(String payload) throws MojoExecutionException {
    Gson gson = new Gson();
    try {
      TargetServer target = gson.fromJson(payload, TargetServer.class);
      return target.name;
    } catch (JsonParseException e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }

  private void doUpdate(List<String> targets) throws MojoExecutionException {
    try {
      List existingTargets = null;
      if (buildOption != OPTIONS.update &&
          buildOption != OPTIONS.create &&
          buildOption != OPTIONS.delete &&
          buildOption != OPTIONS.sync) {
        return;
      }

      logger.info("Retrieving existing environment Target Servers - " +
          serverProfile.getEnvironment());
      existingTargets = getTarget(serverProfile);

      for (String target : targets) {
        String targetName = getTargetName(target);
        if (targetName == null) {
          throw new IllegalArgumentException(
              "Target Server does not have a name.\n" + target + "\n");
        }

        if (existingTargets.contains(targetName)) {
          switch (buildOption) {
            case update:
              logger.info("Target Server \"" + targetName +
                  "\" exists. Updating.");
              updateTarget(serverProfile, targetName, target);
              break;
            case create:
              logger.info("Target Server \"" + targetName +
                  "\" already exists. Skipping.");
              break;
            case delete:
              logger.info("Target Server \"" + targetName +
                  "\" already exists. Deleting.");
              deleteTarget(serverProfile, targetName);
              break;
            case sync:
              logger.info("Target Server \"" + targetName +
                  "\" already exists. Deleting and recreating.");
              deleteTarget(serverProfile, targetName);
              logger.info("Creating Target Server - " + targetName);
              createTarget(serverProfile, target);
              break;
          }
        } else {
          switch (buildOption) {
            case create:
            case sync:
            case update:
              logger.info("Creating Target Server - " + targetName);
              createTarget(serverProfile, target);
              break;
            case delete:
              logger.info("Target Server \"" + targetName +
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

    Logger logger = LoggerFactory.getLogger(TargetServerMojo.class);

    init();

    if (buildOption == OPTIONS.none) {
      logger.info("Skipping Target Servers (default action)");
      return;
    }

    if (serverProfile.getEnvironment() == null) {
      throw new MojoExecutionException(
          "Apigee environment not found in profile");
    }

    List targets = getEnvConfig(logger, "targetServers");
    if (targets == null || targets.size() == 0) {
      logger.info(
          "No target server config found.");
      return;
    }

    doUpdate(targets);
  }

  /***************************************************************************
   * REST call wrappers
   **/
  private static void createTarget(ServerProfile profile, String target)
      throws IOException {

    HttpResponse response = RestUtil.createEnvConfig(profile,
        "targetservers",
        target);
    try {

      logger.info("Response " + response.getContentType() + "\n" +
          response.parseAsString());
      if (response.isSuccessStatusCode())
        logger.info("Create Success.");

    } catch (HttpResponseException e) {
      logger.error("Target Server create error " + e.getMessage());
      throw new IOException(e.getMessage());
    }

  }

  private static void updateTarget(ServerProfile profile,
                                   String targetName,
                                   String target)
      throws IOException {

    HttpResponse response = RestUtil.updateEnvConfig(profile,
        "targetservers",
        targetName,
        target);
    try {

      logger.info("Response " + response.getContentType() + "\n" +
          response.parseAsString());
      if (response.isSuccessStatusCode())
        logger.info("Update Success.");

    } catch (HttpResponseException e) {
      logger.error("Target Server update error " + e.getMessage());
      throw new IOException(e.getMessage());
    }

  }

  private static void deleteTarget(ServerProfile profile,
                                   String targetName)
      throws IOException {

    HttpResponse response = RestUtil.deleteEnvConfig(profile,
        "targetservers",
        targetName);
    try {

      logger.info("Response " + response.getContentType() + "\n" +
          response.parseAsString());
      if (response.isSuccessStatusCode())
        logger.info("Delete Success.");

    } catch (HttpResponseException e) {
      logger.error("Target Server delete error " + e.getMessage());
      throw new IOException(e.getMessage());
    }

  }

  private static List getTarget(ServerProfile profile)
      throws IOException {

    HttpResponse response = RestUtil.getEnvConfig(profile, "targetservers");
    if (response == null) return new ArrayList();
    JSONArray targets = null;
    try {
      logger.debug("output " + response.getContentType());
      // response can be read only once
      String payload = response.parseAsString();
      logger.debug(payload);

      /* Parsers fail to parse a string array.
       * converting it to an JSON object as a workaround */
      String obj = "{ \"targets\": " + payload + "}";

      JSONParser parser = new JSONParser();
      JSONObject obj1 = (JSONObject) parser.parse(obj);
      targets = (JSONArray) obj1.get("targets");

    } catch (ParseException pe) {
      logger.error("Get Target Server parse error " + pe.getMessage());
      throw new IOException(pe.getMessage());
    } catch (HttpResponseException e) {
      logger.error("Get Target Server error " + e.getMessage());
      throw new IOException(e.getMessage());
    }

    return targets;
  }
}




