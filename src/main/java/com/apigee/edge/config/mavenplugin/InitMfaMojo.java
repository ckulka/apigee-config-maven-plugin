/**
 * Copyright (C) 2014 Apigee Corporation
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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;


/**
 * Goal to initialise multifactor authentication / oauth token
 *
 * @author ssvaidyanathan
 */
@Mojo(name = "initmfa", defaultPhase = LifecyclePhase.VALIDATE)
@Execute(lifecycle = "validate")
class InitMfaMojo extends GatewayAbstractMojo {

    /**
     * Entry point for the mojo.
     */
    public void execute() throws MojoExecutionException {
        try {
            RestUtil.initMfa(this.getProfile());
        } catch (Exception e) {
            throw new MojoExecutionException("", e);
        }
    }

}
