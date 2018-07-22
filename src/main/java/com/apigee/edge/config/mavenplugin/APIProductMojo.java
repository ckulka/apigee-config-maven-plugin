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

/**
 * ¡¡
 * Goal to create API Product in Apigee EDGE
 * scope: org
 *
 * @author madhan.sadasivam
 */
@Mojo(name = "apiproducts", defaultPhase = LifecyclePhase.INSTALL)
public class APIProductMojo extends GatewayAbstractMojo {
  private static Logger logger = LoggerFactory.getLogger(APIProductMojo.class);
  private static final String ____ATTENTION_MARKER____ =
      "************************************************************************";

  enum OPTIONS {
    none, create, update, delete, sync
  }

  private OPTIONS buildOption = OPTIONS.none;

  private ServerProfile serverProfile;

  static class APIProduct {
    @Key
    String name;
  }

  private void init() {
    try {
      logger.info(____ATTENTION_MARKER____);
      logger.info("Apigee API Product");
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

  private String getAPIProductName(String payload)
      throws MojoFailureException {
    Gson gson = new Gson();
    try {
      APIProduct product = gson.fromJson(payload, APIProduct.class);
      return product.name;
    } catch (JsonParseException e) {
      throw new MojoFailureException(e.getMessage());
    }
  }

  private void doUpdate(List<String> products)
      throws MojoFailureException {
    try {
      List existingAPIProducts;
      if (buildOption != OPTIONS.update &&
          buildOption != OPTIONS.create &&
          buildOption != OPTIONS.delete &&
          buildOption != OPTIONS.sync) {
        return;
      }

      logger.info("Retrieving existing API Products");
      existingAPIProducts = getAPIProduct(serverProfile);

      for (String product : products) {
        String productName = getAPIProductName(product);
        if (productName == null) {
          throw new IllegalArgumentException(
              "API Product does not have a name.\n" + product + "\n");
        }

        if (existingAPIProducts.contains(productName)) {
          switch (buildOption) {
            case update:
              logger.info("API Product \"" + productName +
                  "\" exists. Updating.");
              updateAPIProduct(serverProfile, productName, product);
              break;
            case create:
              logger.info("API Product \"" + productName +
                  "\" already exists. Skipping.");
              break;
            case delete:
              logger.info("API Product \"" + productName +
                  "\" already exists. Deleting.");
              deleteAPIProduct(serverProfile, productName);
              break;
            case sync:
              logger.info("API Product \"" + productName +
                  "\" already exists. Deleting and recreating.");
              deleteAPIProduct(serverProfile, productName);
              logger.info("Creating API Product - " + productName);
              createAPIProduct(serverProfile, product);
              break;
          }
        } else {
          switch (buildOption) {
            case create:
            case sync:
            case update:
              logger.info("Creating API Product - " + productName);
              createAPIProduct(serverProfile, product);
              break;
            case delete:
              logger.info("API Product \"" + productName +
                  "\" does not exist. Skipping.");
              break;
          }
        }
      }

    } catch (IOException e) {
      throw new MojoFailureException("Apigee network call error " + e.getMessage());
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

    Logger logger = LoggerFactory.getLogger(APIProductMojo.class);

    init();

    if (buildOption == OPTIONS.none) {
      logger.info("Skipping API Products (default action)");
      return;
    }

    if (serverProfile.getEnvironment() == null) {
      throw new MojoExecutionException(
          "Apigee environment not found in profile");
    }

    List products = getOrgConfig(logger, "apiProducts");
    if (products == null || products.size() == 0) {
      logger.info("No API Products found.");
      return;
    }

    doUpdate(products);

  }

  /***************************************************************************
   * REST call wrappers
   **/
  private static void createAPIProduct(ServerProfile profile, String product)
      throws IOException {

    HttpResponse response = RestUtil.createOrgConfig(profile,
        "apiproducts",
        product);
    try {

      logger.info("Response " + response.getContentType() + "\n" +
          response.parseAsString());
      if (response.isSuccessStatusCode())
        logger.info("Create Success.");

    } catch (HttpResponseException e) {
      logger.error("API Product create error " + e.getMessage());
      throw new IOException(e.getMessage());
    }

  }

  private static void updateAPIProduct(ServerProfile profile,
                                       String productName,
                                       String product)
      throws IOException {

    HttpResponse response = RestUtil.updateOrgConfig(profile,
        "apiproducts",
        productName,
        product);
    try {

      logger.info("Response " + response.getContentType() + "\n" +
          response.parseAsString());
      if (response.isSuccessStatusCode())
        logger.info("Update Success.");

    } catch (HttpResponseException e) {
      logger.error("API Product update error " + e.getMessage());
      throw new IOException(e.getMessage());
    }

  }

  private static void deleteAPIProduct(ServerProfile profile,
                                       String productName)
      throws IOException {

    HttpResponse response = RestUtil.deleteOrgConfig(profile,
        "apiproducts",
        productName);
    try {

      logger.info("Response " + response.getContentType() + "\n" +
          response.parseAsString());
      if (response.isSuccessStatusCode())
        logger.info("Delete Success.");

    } catch (HttpResponseException e) {
      logger.error("API Product delete error " + e.getMessage());
      throw new IOException(e.getMessage());
    }

  }

  private static List getAPIProduct(ServerProfile profile)
      throws IOException {

    HttpResponse response = RestUtil.getOrgConfig(profile, "apiproducts");
    if (response == null) return new ArrayList();
    try {
      logger.debug("output " + response.getContentType());
      // response can be read only once
      String payload = response.parseAsString();
      logger.debug(payload);

      /* Parsers fail to parse a string array.
       * converting it to an JSON object as a workaround */
      String obj = "{ \"products\": " + payload + "}";

      JSONParser parser = new JSONParser();
      JSONObject obj1 = (JSONObject) parser.parse(obj);
      return (JSONArray) obj1.get("products");

    } catch (ParseException pe) {
      logger.error("Get API Product parse error " + pe.getMessage());
      throw new IOException(pe.getMessage());
    } catch (HttpResponseException e) {
      logger.error("Get API Product error " + e.getMessage());
      throw new IOException(e.getMessage());
    }
  }
}




