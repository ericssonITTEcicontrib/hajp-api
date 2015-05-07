package com.ericsson.jenkinsci.hajp.api;

import com.google.common.io.Files;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
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
     * write xml file to disk and reload the corresponding global config
     *
     * @param fileName
     * @param fileAsByteArray
     * @throws IOException
     */
    public void updateGlobalConfig(String fileName, byte[] fileAsByteArray)
        throws IOException, ReactorException, InterruptedException {
        Jenkins jenkins = getJenkins();
        File rootDir = jenkins.getRootDir();

        // write to disk
        File file = new File(rootDir, fileName);
        log.debug("write to file: " + file.getAbsolutePath());
        Files.write(fileAsByteArray, file);

        // reload config
        log.warn("jenkins.reload()" );
        jenkins.reload();
    }

    /**
     * Utility method for sending large number of global configurations in one shot.
     * It takes bundled global configurations from map and passes on to singular processing
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
            updateGlobalConfig(fileName, fileAsByteArray);
        }
    }
}
