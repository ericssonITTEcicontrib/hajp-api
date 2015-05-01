package com.ericsson.jenkinsci.hajp.api;

import com.ericsson.jenkins.hajp.api.Messages;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.util.HudsonIsLoading;
import hudson.util.IOUtils;
import jenkins.model.Jenkins;
import lombok.Getter;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class consists of instance methods that will be utilized by HAJP-CORE
 * and features to access and modify internal Jenkins core job related
 * functionalities.
 */
public class JobsManager {

    public static final String ATTRIBUTE_APP = "app";
    public static final String JOBS_DIRNAME = "jobs";

    @Getter private Jenkins jenkins;

    /**
     * Constructor.
     *
     * @param jenkins the Jenkins instance
     */
    public JobsManager(Jenkins jenkins) {
        this.jenkins = jenkins;
    }

    /**
     * @return the directory
     */
    public File getJobsDir() {
        return new File(jenkins.getRootDir(), JOBS_DIRNAME);
    }

    /**
     * @return the directory of the job
     */
    public File getJobDir(String jobName) {
        return getJobsDir().toPath().resolve(jobName).toFile();
    }

    /**
     * @param jobName
     * @return true if the job exists, false otherwise
     */
    public boolean jobExists(String jobName) {
        return getJobNames().contains(jobName);
    }

    /**
     * @param jobName the job name
     * @return the TopLevelItem item
     * @throws JobManagementException if failed to find the item
     */
    public TopLevelItem getTopLevelItem(String jobName) throws JobManagementException {
        TopLevelItem item = jenkins.getItem(jobName);
        if (item == null) {
            throw new JobManagementException(Messages.jobs_find_not_found(jobName), jobName);
        }
        return item;
    }

    /**
     * @param jobName the job name
     * @return the AbstractProject object
     * @throws JobManagementException if failed to find the project
     */
    public AbstractProject getAbstractProject(String jobName) throws JobManagementException {
        TopLevelItem item = getTopLevelItem(jobName);
        if (item instanceof AbstractItem) {
            return (AbstractProject) item;
        }
        throw new JobManagementException(Messages.jobs_item_subtype_not_matched(jobName));
    }

    /**
     * @param jobName the job name
     * @return the FreeStyleProject object
     * @throws JobManagementException if failed to find the project
     */
    public FreeStyleProject getFreeStyleProject(String jobName) throws JobManagementException {
        TopLevelItem item = getTopLevelItem(jobName);
        if (item instanceof FreeStyleProject) {
            return (FreeStyleProject) item;
        }
        throw new JobManagementException(Messages.jobs_item_subtype_not_matched(jobName));
    }

    /**
     * @param jobName the job name
     * @return the Job object
     * @throws JobManagementException if failed to find the job
     */
    public Job<?, ?> getJob(String jobName) throws JobManagementException {
        TopLevelItem item = getTopLevelItem(jobName);
        if (item instanceof Job) {
            return (Job<?, ?>) item;
        }
        throw new JobManagementException(Messages.jobs_item_subtype_not_matched(jobName));
    }

    /**
     * @return the list of jobs in Jenkins
     */
    public List<TopLevelItem> listJobs() {
        return jenkins.getAllItems(TopLevelItem.class);
    }

    /**
     * @return the collection of jobs in Jenkins
     */
    public Collection<String> getJobNames() {
        return jenkins.getJobNames();
    }

    /**
     * @param item the job item from which to parse the xml file content
     * @return the xml content
     * @throws IOException if failed to get the config xml file content
     */
    public String getJobConfigAsXml(Item item) throws JobManagementException {
        try {
            return Items.getConfigFile(item).asString();
        } catch (IOException e) {
            throw new JobManagementException(Messages.jobs_read_content_error(item.getName()), e,
                item.getName());
        }
    }

    /**
     * @return all job's xml as a Map
     * @throws IOException if any
     */
    public Map<String, String> getJobsXmlAsMap() throws JobManagementException {
        Map<String, String> jobsMap = new HashMap<>();
        for (TopLevelItem job : listJobs()) {
            jobsMap.put(job.getName(), getJobConfigAsXml(job));
        }
        return jobsMap;
    }

    /**
     * @param jobName        the job name
     * @param xmlFileContent the xml file content
     * @throws JobManagementException if failed to create the job from the xml content
     */
    public void createJob(String jobName, String xmlFileContent) throws JobManagementException {
        try {
            jenkins.createProjectFromXML(jobName, IOUtils.toInputStream(xmlFileContent));
        } catch (IOException e) {
            throw new JobManagementException(Messages.jobs_create_from_xml_error(jobName), e,
                jobName, xmlFileContent);
        }
    }

    /**
     * Update a job from a xml file content as string.
     *
     * @param jobName        the job name
     * @param xmlFileContent the xml file content
     * @throws JobManagementException if failed to update the job item
     */
    public void updateJobConfig(String jobName, String xmlFileContent)
        throws JobManagementException {
        try {
            AbstractProject project = getAbstractProject(jobName);
            Path configFile = project.getConfigFile().getFile().toPath();
            Files.write(configFile, xmlFileContent.getBytes());
            project.doReload();
        } catch (IOException e) {
            throw new JobManagementException(Messages.jobs_update_failed(jobName, xmlFileContent),
                e, jobName, xmlFileContent);
        }
    }

    /**
     * @param oldName the old name
     * @param newName the new name
     * @throws JobManagementException if failed to rename the job item
     */
    public void renameJob(String oldName, String newName) throws JobManagementException {
        Job<?, ?> job = getJob(oldName);
        try {
            job.renameTo(newName);
        } catch (IOException e) {
            throw new JobManagementException(Messages.jobs_rename_failed(oldName, newName), e,
                oldName, newName);
        }
    }

    /**
     * @param jobName the job name
     * @throws JobManagementException if failed to delete the job item
     */
    public void deleteJob(String jobName) throws JobManagementException {
        TopLevelItem item = jenkins.getItem(jobName);
        if (item == null) {
            throw new JobManagementException(Messages.jobs_find_not_found(jobName), jobName);
        }
        try {
            item.delete();
        } catch (IOException | InterruptedException e) {
            throw new JobManagementException(Messages.jobs_load_from_filesystem_error(), e,
                e.getMessage());
        }
    }

    /**
     * @param jobName the job name
     * @return the job
     * @throws JobManagementException if failed to reload the job
     */
    public void reloadJob(String jobName) throws JobManagementException {
        FreeStyleProject project;
        try {
            Job<?, ?> job = getJob(jobName);
            job.doReload();
            project = getFreeStyleProject(jobName);
            project.doReload();
        } catch (IOException | JobManagementException e) {
            throw new JobManagementException(Messages.jobs_reload_error(jobName), e, jobName);
        }
    }

    /**
     * Load all jobs from the jenkins home dir.
     *
     * @throws JobManagementException if failed to load any job in Jenkins
     */
    public void loadAllJobs() throws JobManagementException {
        jenkins.servletContext.setAttribute(ATTRIBUTE_APP, new HudsonIsLoading());
        File jobsDir = getJobsDir();
        File[] files = jobsDir.listFiles();
        if (files == null) {
            throw new JobManagementException(Messages.jobs_dir_null());
        }
        try {
            for (File f : files) {
                TopLevelItem item = (TopLevelItem) Items.load(jenkins, f);
            }
        } catch (IOException e) {
            throw new JobManagementException(Messages.jobs_load_all_error(), e, e.getMessage());
        }
        jenkins.servletContext.setAttribute(ATTRIBUTE_APP, jenkins);
    }

    /**
     * Delete all jobs from the jenkins home dir.
     *
     * @throws JobManagementException if failed to delete any job in Jenkins
     */
    public void deleteAllJobs() throws JobManagementException {
        jenkins.servletContext.setAttribute(ATTRIBUTE_APP, new HudsonIsLoading());

        for (String jobName : getJobNames()) {
            deleteJob(jobName);
        }

        jenkins.servletContext.setAttribute(ATTRIBUTE_APP, jenkins);
    }

}
