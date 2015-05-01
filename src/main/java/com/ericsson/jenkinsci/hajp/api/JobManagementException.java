package com.ericsson.jenkinsci.hajp.api;

import lombok.Getter;

/**
 * A custom exception class to handle job's related operation error.
 */
public class JobManagementException extends Exception {

    @Getter private String itemName;
    @Getter private String content;

    /**
     * @param message the message to be carried by the exception
     * @see java.lang.Exception
     */
    public JobManagementException(String message) {
        super(message);
    }

    /**
     * Extended constructor by adding the item name
     * @param message the message to be carried by the exception
     * @param throwable the throwable
     * @param itemName the name of the item which causes the exception
     */
    public JobManagementException(String message, Throwable throwable, String itemName) {
        super(message, throwable);
        this.itemName = itemName;
    }

    /**
     * Extended constructor by adding the item name and content
     * @param message the message to be carried by the exception
     * @param throwable the throwable
     * @param itemName the name of the item which causes the exception
     * @param content the content of the item which causes the exception
     */
    public JobManagementException(String message, Throwable throwable, String itemName, String content) {
        super(message, throwable);
        this.itemName = itemName;
        this.content = content;
    }

    /**
     * Constructor with the item name
     * @param message the message to be carried by the exception
     * @param itemName the name of the item which causes the exception
     */
    public JobManagementException(String message, String itemName) {
        super(message);
        this.itemName = itemName;
    }
}
