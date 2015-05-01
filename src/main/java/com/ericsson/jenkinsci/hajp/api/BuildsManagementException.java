package com.ericsson.jenkinsci.hajp.api;

import lombok.Getter;

/**
 * A custom exception class to handle build's related operation error.
 */
public class BuildsManagementException extends Exception {

    @Getter private String jobName;
    @Getter private int buildNumber;

    /**
     * @param message the message to be carried by the exception
     * @see Exception
     */
    public BuildsManagementException(String message) {
        super(message);
    }

    /**
     * Extended constructor by adding the job name and build number
     *
     * @param message   the message to be carried by the exception
     * @param throwable the throwable
     * @param jobName   the name of the job which causes the exception
     */
    public BuildsManagementException(String message, Throwable throwable, String jobName) {
        super(message, throwable);
        this.jobName = jobName;
    }

    /**
     * Extended constructor by adding the job name and build number
     *
     * @param message     the message to be carried by the exception
     * @param throwable   the throwable
     * @param jobName     the name of the job which causes the exception
     * @param buildNumber the build number
     */
    public BuildsManagementException(String message, Throwable throwable, String jobName,
        int buildNumber) {
        super(message, throwable);
        this.jobName = jobName;
        this.buildNumber = buildNumber;
    }

    /**
     * Constructor with the job name
     *
     * @param message the message to be carried by the exception
     * @param jobName the name of the job which causes the exception
     */
    public BuildsManagementException(String message, String jobName) {
        super(message);
        this.jobName = jobName;
    }

    /**
     * Constructor with the job name and build number
     *
     * @param message the message to be carried by the exception
     * @param jobName the name of the job which causes the exception
     * @param buildNumber the build number
     */
    public BuildsManagementException(String message, String jobName, int buildNumber) {
        super(message);
        this.jobName = jobName;
        this.buildNumber = buildNumber;
    }
}
