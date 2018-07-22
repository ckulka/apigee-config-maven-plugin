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
package com.apigee.edge.config.rest;

import org.apache.axis2.transport.nhttp.HostnameVerifier;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.security.cert.X509Certificate;

class FakeHostnameVerifier implements HostnameVerifier {

  public void check(String arg0, SSLSocket arg1) {
    // TODO Auto-generated method stub

  }

  public void check(String arg0, X509Certificate arg1) {
    // TODO Auto-generated method stub

  }

  public void check(String[] arg0, SSLSocket arg1) {
    // TODO Auto-generated method stub

  }

  public void check(String[] arg0, X509Certificate arg1) {
    // TODO Auto-generated method stub

  }

  public void check(String arg0, String[] arg1, String[] arg2) {
    // TODO Auto-generated method stub

  }

  public void check(String[] arg0, String[] arg1, String[] arg2) {
    // TODO Auto-generated method stub

  }

  public boolean verify(String arg0, SSLSession arg1) {
    // TODO Auto-generated method stub
    return true;
  }

}
