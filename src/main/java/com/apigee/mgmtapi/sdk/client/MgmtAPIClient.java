package com.apigee.mgmtapi.sdk.client;

import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.apigee.mgmtapi.sdk.core.AppConfig;
import com.apigee.mgmtapi.sdk.model.AccessToken;
import com.apigee.mgmtapi.sdk.service.FileService;
import com.google.gson.Gson;

import javax.annotation.Nonnull;

public class MgmtAPIClient {

    private static final Logger logger = Logger.getLogger(MgmtAPIClient.class);


    /**
     * To get the Access Token Management URL, client_id and client_secret needs
     * to be passed through a config file whose full path is passed as system
     * property like -DconfigFile.path="/to/dir/config.properties"
     *
     * @param username username
     * @param password password
     * @return new access token
     */
    public AccessToken getAccessToken(String username, String password) {
        Environment env = this.getConfigProperties();
        return getAccessToken(env.getProperty("mgmt.login.url"), env.getProperty("mgmt.login.client.id"),
                env.getProperty("mgmt.login.client.secret"), username, password);
    }

    /**
     * To get the Access Token Management URL, client_id and client_secret needs
     * to be passed through a config file whose full path is passed as system
     * property like -DconfigFile.path="/to/dir/config.properties"
     *
     * @param username username
     * @param password password
     * @param mfa      multi-factor token
     * @return new access token
     */
    public AccessToken getAccessToken(String username, String password, String mfa) {
        Environment env = this.getConfigProperties();
        if (mfa == null || mfa.equals("")) {
            logger.error("mfa cannot be empty");
            throw new IllegalArgumentException("mfa cannot be empty");
        }
        return getAccessToken(env.getProperty("mgmt.login.mfa.url") + mfa, env.getProperty("mgmt.login.client.id"),
                env.getProperty("mgmt.login.client.secret"), username, password);
    }


    /**
     * To get Access Token
     *
     * @param url           OAuth endpoint
     * @param clientId      OAuth client id
     * @param client_secret OAuth client secret
     * @param username      username
     * @param password      password
     * @param mfa           multi-factor token
     * @return new access token
     */
    public AccessToken getAccessToken(String url, String clientId, String client_secret, String username,
                                      String password, String mfa) {
        return getAccessToken(url + "?mfa_token=" + mfa, clientId, client_secret, username, password);
    }

    /**
     * To get the Access Token
     *
     * @param url           OAuth endpoint
     * @param clientId      OAuth client id
     * @param client_secret OAuth client secret
     * @param username      username
     * @param password      password
     * @return new access token
     */
    public AccessToken getAccessToken(String url, String clientId, String client_secret, String username,
                                      String password) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        AccessToken token = new AccessToken();
        ResponseEntity<String> result;
        try {
            headers.add("Authorization", "Basic "
                    + new String(Base64.encode((clientId + ":" + client_secret).getBytes()), Charset.forName("UTF-8")));
            headers.add("Content-Type", "application/x-www-form-urlencoded");
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("username", username);
            map.add("password", password);
            map.add("grant_type", "password");
            HttpEntity<Object> request = new HttpEntity<>(map, headers);
            result = restTemplate.postForEntity(url, request, String.class);
            if (result.getStatusCode().equals(HttpStatus.OK)) {
                Gson gson = new Gson();
                token = gson.fromJson(result.getBody(), AccessToken.class);

            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
        return token;

    }

    /**
     * To get the Access Token from Refresh Token
     *
     * @param url           OAuth endpoint
     * @param clientId      OAuth client id
     * @param client_secret OAuth client secret
     * @param refreshToken  OAuth refresh token
     * @return new access token
     */
    public AccessToken getAccessTokenFromRefreshToken(String url, String clientId, String client_secret, String refreshToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<String> result;
        try {
            headers.add("Authorization", "Basic "
                    + new String(Base64.encode((clientId + ":" + client_secret).getBytes()), Charset.forName("UTF-8")));
            headers.add("Content-Type", "application/x-www-form-urlencoded");
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("refresh_token", refreshToken);
            map.add("grant_type", "refresh_token");
            HttpEntity<Object> request = new HttpEntity<>(map, headers);
            result = restTemplate.postForEntity(url, request, String.class);
            if (result.getStatusCode().equals(HttpStatus.OK)) {
                return new Gson().fromJson(result.getBody(), AccessToken.class);
            } else {
                return new AccessToken();
            }
        } catch (Exception e) {
            logger.error("Refresh Token could be invalid or expired: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fetch the properties from the property file passed as system argument (-DconfigFile.path)
     *
     * @return Spring application context environment
     */
    @Nonnull
    private Environment getConfigProperties() {
        if (System.getProperty("configFile.path") != null
                && !System.getProperty("configFile.path").equalsIgnoreCase("")) {
            AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
            return ((FileService) context.getBean("fileService")).getEnvironment();
        } else {
            logger.error("Config file missing");
            throw new IllegalArgumentException("Config file missing");
        }
    }
}
