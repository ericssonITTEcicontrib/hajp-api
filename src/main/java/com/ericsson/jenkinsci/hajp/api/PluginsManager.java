package com.ericsson.jenkinsci.hajp.api;

import com.google.common.io.Files;
import hudson.XmlFile;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Log4j2 public class PluginsManager {
    @Getter private Jenkins jenkins;

    /**
     * Constructor.
     *
     * @param jenkins the Jenkins instance
     */
    public PluginsManager(Jenkins jenkins) {
        this.jenkins = jenkins;
    }

    /**
     * write xml file to disk and load the corresponding descriptor(if it
     * exists)
     *
     * @param pluginName
     * @param fileName
     * @param fileAsByteArray
     * @throws java.io.IOException
     */
    public void updatePluginConfig(String pluginName, String fileName, byte[] fileAsByteArray)
        throws IOException {
        Jenkins jenkins = getJenkins();
        File rootDir = jenkins.getRootDir();
        File file;
        String id;
        Descriptor<?> d;

        // write to disk
        file = new File(rootDir, fileName);
        Files.write(fileAsByteArray, file);

        // find correct Descriptor
        id = FilenameUtils.removeExtension(pluginName);
        d = jenkins.getDescriptor(id);

        if (d == null) {
            pluginName = pluginName.replace("-", "");
            pluginName = pluginName.replace(" ", "");
            d = jenkins.getDescriptor(pluginName);
        }

        // if it exists, unmarshal the file into the Descriptor(load into
        // memory)
        if (d != null) {
            log.warn("Found Descriptor with id: {} or {}", id, pluginName);
            try {
                new XmlFile(file).unmarshal(d);
            } catch (Exception e) {
                log.warn("Caught Exception while unmarshalling config file for plugin: {}", id,
                    e.getMessage());
            }
            // restore to null just in case
            d = null;
        } else {
            log.warn("Could not find and load Descriptor with id: {}", id);
        }
    }

    /**
     * Utility method for sending large number of plugin configuration in one shot.
     * It takes bundled plugin configuration fro map and passes on to singular processing
     * method with filename, id and content information extracted and separated.
     *
     * @param filesMap
     * @throws java.io.IOException
     */
    public void updatePluginsConfig(Map<String, byte[]> filesMap) throws IOException {
        for (Map.Entry<String, byte[]> e : filesMap.entrySet()) {
            String fileName = e.getKey();
            byte[] fileAsByteArray = e.getValue();

            String id = FilenameUtils.removeExtension(fileName);
            updatePluginConfig(id, fileName, fileAsByteArray);
        }
    }
}
