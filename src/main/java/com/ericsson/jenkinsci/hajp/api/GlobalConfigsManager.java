package com.ericsson.jenkinsci.hajp.api;

import com.google.common.io.Files;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.jvnet.hudson.reactor.ReactorException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Log4j2 public class GlobalConfigsManager {
    @Getter private Jenkins jenkins;

    /**
     * Constructor.
     *
     * @param jenkins the Jenkins instance
     */
    public GlobalConfigsManager(Jenkins jenkins) {
        this.jenkins = jenkins;
    }

    /**
     * write xml file to disk and load the corresponding descriptor(if it
     * exists)
     *
     * @param pluginName
     * @param fileName
     * @param fileAsByteArray
     * @throws IOException
     */
    public void updateGlobalConfig(String pluginName, String fileName, byte[] fileAsByteArray)
        throws IOException, ReactorException, InterruptedException {
        Jenkins jenkins = getJenkins();
        File rootDir = jenkins.getRootDir();
        File file;
        String id;
        Descriptor<?> d;

        // write to disk
        file = new File(rootDir, fileName);
        log.debug("write to file: " + file.getAbsolutePath());
        Files.write(fileAsByteArray, file);

        // reload config
        log.warn("jenkins.reload()" );
        jenkins.reload();
    }

    /**
     * Utility method for sending large number of plugin configuration in one shot.
     * It takes bundled plugin configuration fro map and passes on to singular processing
     * method with filename, id and content information extracted and separated.
     *
     * @param filesMap
     * @throws IOException
     */
    public void updateGlobalConfig(Map<String, byte[]> filesMap)
        throws IOException, InterruptedException, ReactorException {
        for (Map.Entry<String, byte[]> e : filesMap.entrySet()) {
            String fileName = e.getKey();
            byte[] fileAsByteArray = e.getValue();

            String id = FilenameUtils.removeExtension(fileName);
            updateGlobalConfig(id, fileName, fileAsByteArray);
        }
    }
}
