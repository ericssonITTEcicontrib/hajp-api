package com.ericsson.jenkinsci.hajp.api;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Items;
import hudson.model.TopLevelItem;
import hudson.util.IOUtils;
import jenkins.model.AbstractTopLevelItem;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * PowerMock Binary Modification reasoning
 * IOUtils, Items, XmlFile classes contain static methods required for testing
 * Jenkins class had to modify servlet context
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({IOUtils.class, Items.class, XmlFile.class, Jenkins.class})
public class JobsManagerTest {

    private final static String MOCK_JOB_NAME = "Test";
    private final static String XML_FILE_CONTENT = "</xml>";

    @Rule public TemporaryFolder tmpFile = new TemporaryFolder();

    JobsManager unitUnderTest;
    Jenkins mockJenkins;
    TopLevelItem mockItem;
    XmlFile mockXmlFile;
    TopLevelItem mockTopLevelItem;
    AbstractProject mockAbstractItem;
    FreeStyleProject mockFreeStyleProject;
    ServletContext mockServletContext;
    InputStream mockByteInput;

    @Before public void setUp() throws Exception {
        mockJenkins = PowerMockito.mock(Jenkins.class);
        unitUnderTest = new JobsManager(mockJenkins);
        mockByteInput = Mockito.mock(InputStream.class);
        mockItem = Mockito.mock(TopLevelItem.class);
        mockTopLevelItem = Mockito.mock(TopLevelItem.class);
        mockAbstractItem = Mockito.mock(AbstractProject.class);
        mockFreeStyleProject = Mockito.mock(FreeStyleProject.class);
        mockServletContext = Mockito.mock(ServletContext.class);

        Whitebox.setInternalState(mockJenkins, "servletContext", mockServletContext);

        // classes with static methods and/or final classes mocked
        PowerMockito.mockStatic(IOUtils.class);
        PowerMockito.mockStatic(Items.class);
        mockXmlFile = PowerMockito.mock(XmlFile.class);

        Mockito.when(IOUtils.toInputStream(XML_FILE_CONTENT)).thenReturn(mockByteInput);
        Mockito.when(Items.getConfigFile(mockItem)).thenReturn(mockXmlFile);
        Mockito.when(mockXmlFile.asString()).thenReturn("test");
        Mockito.when(mockJenkins.getItem(MOCK_JOB_NAME)).thenReturn(mockTopLevelItem);
    }

    @Test public void testGetJobsDir() throws Exception {
        unitUnderTest.getJobsDir();
        Mockito.verify(mockJenkins).getRootDir();
    }

    @Test public void testGetJobItem() throws Exception {
        unitUnderTest.getTopLevelItem(MOCK_JOB_NAME);
        Mockito.verify(mockJenkins).getItem(MOCK_JOB_NAME);
    }

    @Test public void testGetJobContentAsXml() throws Exception {
        assertEquals("test", unitUnderTest.getJobConfigAsXml(mockItem));
    }

    @Test public void testGetJobsXmlAsMap() throws Exception {
        List<TopLevelItem> list = new ArrayList<>();
        list.add(mockItem);
        Mockito.when(mockJenkins.getAllItems(TopLevelItem.class)).thenReturn(list);
        Map<String, String> jobsMap = unitUnderTest.getJobsXmlAsMap();
        assertEquals(1, jobsMap.size());
        assertEquals("test", jobsMap.get(mockItem.getName()));
    }

    @Test public void testCreateJob() throws Exception {
        unitUnderTest.createJob(MOCK_JOB_NAME, XML_FILE_CONTENT);
        Mockito.verify(mockJenkins).createProjectFromXML(MOCK_JOB_NAME, mockByteInput);
    }

    @Test public void testUpdateJobConfig() throws Exception {
        tmpFile.create();
        File file = tmpFile.newFile("config.xml");
        XmlFile xmlfile = new XmlFile(file);
        Mockito.when(mockFreeStyleProject.getConfigFile()).thenReturn(xmlfile);
        Mockito.when(mockJenkins.getItem(MOCK_JOB_NAME)).thenReturn(mockFreeStyleProject);
        unitUnderTest.updateJobConfig(MOCK_JOB_NAME, mockXmlFile.asString());
    }

    @Test(expected = JobManagementException.class) public void testUpdateJobConfigWithException() throws Exception {
        unitUnderTest.updateJobConfig(MOCK_JOB_NAME, mockXmlFile.asString());
    }

    @Test public void testRenameJob() throws Exception {
        Mockito.when(mockJenkins.getItem(MOCK_JOB_NAME)).thenReturn(mockFreeStyleProject);
        unitUnderTest.renameJob(MOCK_JOB_NAME, MOCK_JOB_NAME);
    }

    @Test(expected = JobManagementException.class) public void testRenameJobWithException() throws Exception {
        unitUnderTest.renameJob(MOCK_JOB_NAME, MOCK_JOB_NAME);
    }

    @Test public void testDeleteJob() throws Exception {
        unitUnderTest.deleteJob(MOCK_JOB_NAME);
    }

    @Test(expected = JobManagementException.class) public void testDeleteJobWithException()
        throws Exception {
        Mockito.when(mockJenkins.getItem(MOCK_JOB_NAME)).thenReturn(null);
        unitUnderTest.deleteJob(MOCK_JOB_NAME);
    }

    @Test(expected = JobManagementException.class) public void testLoadAllJobsWithException()
        throws Exception {
        unitUnderTest.loadAllJobs();
    }

    @Test public void testLoadAllJobsNoException() throws Exception {
        Mockito.when(mockJenkins.getRootDir()).thenReturn(new File("/tmp"));
        // create sub folder "jobs" in /tmp
        File jobs = new File("/tmp/jobs");
        boolean jobFolderCreated = jobs.mkdir();
        if (jobs.exists() || jobFolderCreated) {
            File dummyFile = new File("/tmp/jobs/dummy.xml");
            boolean fileCreated = dummyFile.createNewFile();
            if (dummyFile.exists() || fileCreated) {
                Mockito.when(Items.load(mockJenkins, dummyFile)).thenReturn(mockFreeStyleProject);
                unitUnderTest.loadAllJobs();
            } else {
                throw new IllegalStateException("Temp dummy file can not be created");
            }
        } else {
            throw new IllegalStateException("Temp Jobs Folder can not be created");
        }
    }
}
