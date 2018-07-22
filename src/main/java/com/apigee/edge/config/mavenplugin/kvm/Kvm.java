package com.apigee.edge.config.mavenplugin.kvm;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;

public interface Kvm {

  void update(KvmValueObject kvmValueObject)
      throws IOException, MojoExecutionException;

}
