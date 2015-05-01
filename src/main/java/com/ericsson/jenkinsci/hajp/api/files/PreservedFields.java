package com.ericsson.jenkinsci.hajp.api.files;

import lombok.Getter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Data class to maintain fields to be preserved on job config change.
 */
@XmlRootElement public class PreservedFields implements Serializable {

    /* List of fields in job config.xml to be preserved on update */
    @XmlElement @Getter private Set<String> jobs = new HashSet<>();

}
