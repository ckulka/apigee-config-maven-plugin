package com.apigee.edge.config.mavenplugin;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


class GatewayAbstractMojoTest {

    private GatewayAbstractMojo gatewayAbstractMojo;

    @BeforeEach
    void before() {
        gatewayAbstractMojo = new GatewayAbstractMojo() {

            @Override
            public void execute() {
                // Noop
            }
        };
        gatewayAbstractMojo.setConfigDir(Paths.get("src/test/resources", getClass().getName()).toString());
    }

    @Test
    void findConfigFileWithMissingFile() {
        File actual = gatewayAbstractMojo.findConfigFile("./", "findConfigFileWithMissingFile");
        Assert.assertNull(actual);
    }

    @Test
    void findConfigFile() {
        File actual = gatewayAbstractMojo.findConfigFile("./", "findConfigFile");
        Assert.assertNotNull(actual);
        Path expected = Paths.get("src/test/resources", getClass().getName(), "./findConfigFile.json");
        Assert.assertEquals(expected.toFile(), actual);
    }

    @Test
    void findConfigFileFromYaml() {
        File actual = gatewayAbstractMojo.findConfigFile("./", "findConfigFileFromYaml");
        Assert.assertNotNull(actual);
        Path expected = Paths.get("src/test/resources", getClass().getName(), "./findConfigFileFromYaml.yaml");
        Assert.assertEquals(expected.toFile(), actual);
    }
}
