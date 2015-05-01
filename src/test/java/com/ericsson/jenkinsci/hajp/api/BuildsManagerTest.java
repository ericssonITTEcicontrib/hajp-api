package com.ericsson.jenkinsci.hajp.api;

import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BuildsManagerTest {

    private static final Format format = new SimpleDateFormat("yyyy-dd-MM_hh-mm-ss");
    private BuildsManager buildsManagerUnderTest;
    private JobsManager mockJobsManager;
    private Jenkins mockJenkins;
    private TopLevelItem mockTopLevelItem;
    private AbstractProject mockAbstractProject;
    private Job mockJob;

    private File rootDir;
    private File jobDir;
    private File buildDir;
    private String jobName;
    private int buildNumber;

    @Rule public TemporaryFolder rule = new TemporaryFolder();

    public BuildsManagerTest() throws IOException {
        mockJenkins = Mockito.mock(Jenkins.class);
        mockAbstractProject = Mockito.mock(AbstractProject.class);
        mockJob = Mockito.mock(Job.class);
        mockTopLevelItem = Mockito.mock(TopLevelItem.class);
        mockJobsManager = Mockito.mock(JobsManager.class);
        buildsManagerUnderTest = new BuildsManager(mockJobsManager);

        jobName = "jobUnderTest";
        buildNumber = 1;
    }

    @Before public void setUp() throws Exception {
        rootDir = rule.newFolder("jobs");
        jobDir = createJobFiles(jobName);
        buildDir = createBuildFiles(jobDir, buildNumber);
        createLinks(jobDir, buildDir);

        Mockito.when(mockJenkins.getRootDir()).thenReturn(rootDir);
        Mockito.when(mockJobsManager.getJob(jobName)).thenReturn(mockJob);
        Mockito.when(mockJobsManager.getJobDir(jobName)).thenReturn(jobDir);
    }

    @After public void after() {
        Mockito.reset(mockAbstractProject);
        Mockito.reset(mockJob);
        Mockito.reset(mockTopLevelItem);
    }

    private File createJobFiles(String jobName) throws IOException {
        File jobDir = new File(rootDir, jobName);
        jobDir.mkdir();
        File buildsDir = new File(jobDir, buildsManagerUnderTest.BUILDS_DIRNAME);
        buildsDir.mkdir();
        File workspace = new File(jobDir, "workspace");
        workspace.mkdir();
        File configXml = new File(jobDir, "config.xml");
        configXml.createNewFile();
        File nextBuildNumber = new File(jobDir, "nextBuildNumber");
        nextBuildNumber.createNewFile();

        return jobDir;
    }

    private File createBuildFiles(File jobDir, int buildNumber) throws IOException {
        File buildsDir = new File(jobDir, buildsManagerUnderTest.BUILDS_DIRNAME);
        String buildDirName = format.format(new Date());
        File buildDir = new File(buildsDir, buildDirName);
        buildDir.mkdir();

        File buildNumberDir = new File(buildsDir, "" + buildNumber);
        Files.createSymbolicLink(buildNumberDir.toPath(), buildDir.toPath());

        File buildXml = new File(buildDir, "build.xml");
        buildXml.createNewFile();
        File changeLog = new File(buildDir, "changelog.xml");
        changeLog.createNewFile();
        File log = new File(buildDir, "log");
        log.createNewFile();

        return buildDir;
    }

    private void createLinks(File jobDir, File buildDir) throws IOException {
        File buildsDir = new File(jobDir, buildsManagerUnderTest.BUILDS_DIRNAME);

        // create symbolic link to build at job level
        Path jobSuccessfulLinkPath =
            Paths.get(jobDir.getAbsolutePath(), BuildsManager.LAST_SUCCESSFUL_BUILD);
        Path jobSuccessfulPath =
            Paths.get(buildDir.getAbsolutePath(), BuildsManager.LAST_SUCCESSFUL_BUILD);
        Files.createSymbolicLink(jobSuccessfulLinkPath, jobSuccessfulPath);

        Path jobStableLinkPath =
            Paths.get(jobDir.getAbsolutePath(), BuildsManager.LAST_STABLE_BUILD);
        Path stablePath = Paths.get(jobDir.getAbsolutePath(), BuildsManager.LAST_STABLE_BUILD);
        Files.createSymbolicLink(jobStableLinkPath, stablePath);

        // create symbolic link to build at builds level
        Path buildsSuccessfulLinkPath =
            Paths.get(buildsDir.getAbsolutePath(), BuildsManager.LAST_SUCCESSFUL_BUILD);
        Files.createSymbolicLink(buildsSuccessfulLinkPath, buildDir.toPath());
        Path buildsStableLinkPath =
            Paths.get(buildsDir.getAbsolutePath(), BuildsManager.LAST_STABLE_BUILD);
        Files.createSymbolicLink(buildsStableLinkPath, buildDir.toPath());
    }

    @Test public void testGetBuildsDir() throws BuildsManagementException {
        Path expected = Paths.get(rootDir.getAbsolutePath(), jobName)
            .resolve(buildsManagerUnderTest.BUILDS_DIRNAME);

        Path buildsDir = buildsManagerUnderTest.getBuildsDir(jobName);

        Assert.assertEquals(expected, buildsDir);
    }

    @Test public void testGetBuildDir() throws BuildsManagementException {
        Path expected = Paths.get(rootDir.getAbsolutePath(), jobName)
            .resolve(buildsManagerUnderTest.BUILDS_DIRNAME).resolve("" + buildNumber);

        Path buildDir = buildsManagerUnderTest.getNumberBuildDir(jobName, buildNumber);

        Assert.assertEquals(expected, buildDir);
    }

    @Test public void testGetTimestampBuildDirName() throws BuildsManagementException {
        String timestampBuildDirName =
            buildsManagerUnderTest.getTimestampBuildDirName(jobName, buildNumber);

        Assert.assertEquals(buildDir.getName(), timestampBuildDirName);
    }

    @Test public void testGrabBuild() throws BuildsManagementException {
        byte[] fileAsByteArray = buildsManagerUnderTest.grabBuild(jobName, buildNumber);
        Assert.assertNotNull(fileAsByteArray);
    }

    @Test public void testExtractFile() throws BuildsManagementException, IOException {
        byte[] fileAsByteArray = buildsManagerUnderTest.grabBuild(jobName, buildNumber);
        String timestampBuildDirName =
            buildsManagerUnderTest.getTimestampBuildDirName(jobName, buildNumber);
        Path destBuildDir =
            buildsManagerUnderTest.getBuildsDir(jobName).resolve(timestampBuildDirName);
        FileUtils.deleteDirectory(destBuildDir.toFile());
        Path numberBuildDir =
            buildsManagerUnderTest.getNumberBuildDir(jobName, buildNumber);
        Files.delete(numberBuildDir);

        destBuildDir = buildsManagerUnderTest
            .extractBuild(fileAsByteArray, jobName, buildNumber, timestampBuildDirName);
        Assert.assertTrue(Files.exists(destBuildDir));
    }

    @Test public void testListBuilds() throws BuildsManagementException {
        RunList runList = Mockito.mock(RunList.class);
        Mockito.when(mockAbstractProject.getBuilds()).thenReturn(runList);
        Assert.assertEquals(runList, buildsManagerUnderTest.listBuilds(mockAbstractProject));
    }

    @Test public void testGetBuild() throws BuildsManagementException {
        Run mockRun = Mockito.mock(Run.class);
        Mockito.when(mockJob.getBuildByNumber(buildNumber)).thenReturn(mockRun);
        Assert.assertEquals(mockRun, buildsManagerUnderTest.getBuild(jobName, buildNumber));
    }

    @Test public void testBuildExists() throws BuildsManagementException {
        Run mockRun = Mockito.mock(Run.class);
        Mockito.when(mockJob.getBuildByNumber(buildNumber)).thenReturn(mockRun);
        Assert.assertEquals(true, buildsManagerUnderTest.buildExists(jobName, buildNumber));
    }
}
