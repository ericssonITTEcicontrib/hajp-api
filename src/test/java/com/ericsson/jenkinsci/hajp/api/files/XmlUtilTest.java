package com.ericsson.jenkinsci.hajp.api.files;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class XmlUtilTest {

    private static String SINGLE_SCM_GIT_CREDENTIAL_XPATH =
        "/project/scm/userRemoteConfigs/hudson.plugins.git.UserRemoteConfig/credentialsId";
    private static String SINGLE_SCM_GIT_URL_XPATH =
        "/project/scm/userRemoteConfigs/hudson.plugins.git.UserRemoteConfig/url";
    private static String MULTIPLE_SCM_GIT_CREDENTIAL_XPATH =
        "/project/scm/scms/hudson.plugins.git.GitSCM/userRemoteConfigs/hudson.plugins.git.UserRemoteConfig/credentialsId";
    private static String MULTIPLE_SCM_GIT_URL_XPATH =
        "/project/scm/scms/hudson.plugins.git.GitSCM/userRemoteConfigs/hudson.plugins.git.UserRemoteConfig/url";

    private static String PRESERVED_FIELDS_STRING =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + "<preservedFields>\n"
            + "    <jobs>credentialsId</jobs>\n" + "</preservedFields>";

    @Rule public TemporaryFolder testFolder = new TemporaryFolder();

    private PreservedFields preservedFields;
    private Path origFilePath;
    private Path incomingFilePath;
    private String newXml;
    private String preservedFieldsXml;

    @Before public void prepareData() throws IOException {
        testFolder.create();
    }

    @Test public void testSaveAndLoadPreservedFields() throws Exception {
        preservedFields = new PreservedFields();
        preservedFields.getJobs().add("credentialsId");

        preservedFieldsXml = XmlUtil.saveToXml(preservedFields);
        Assert.assertEquals(PRESERVED_FIELDS_STRING, preservedFieldsXml.trim());

        PreservedFields result = XmlUtil.xmlToPreservedFields(preservedFieldsXml);
        Assert.assertTrue(result.getJobs().contains("credentialsId"));
    }

    @Test public void testMergeWithNoMatchingXpath() throws Exception {
        preservedFields = new PreservedFields();
        preservedFields.getJobs().add("no matching xpath");

        origFilePath = Paths.get("./src/test/resources", "job_scm_originConfig.xml");
        incomingFilePath = Paths.get("./src/test/resources", "job_scm_incomingConfig.xml");

        String origXml = new String(Files.readAllBytes(origFilePath));
        String incomingXml = new String(Files.readAllBytes(incomingFilePath));
        newXml = XmlUtil.mergeXmlByPreservingField(origXml, incomingXml, preservedFields.getJobs());

        Assert.assertEquals(incomingXml.replaceFirst("\\n", ""), newXml);
    }

    @Test public void testMergeSingleScmWithGitConfig() throws Exception {
        preservedFields = new PreservedFields();
        preservedFields.getJobs().add(SINGLE_SCM_GIT_CREDENTIAL_XPATH);
        preservedFields.getJobs().add(SINGLE_SCM_GIT_URL_XPATH);

        origFilePath = Paths.get("./src/test/resources", "job_scm_originConfig.xml");
        incomingFilePath = Paths.get("./src/test/resources", "job_scm_incomingConfig.xml");

        String origXml = new String(Files.readAllBytes(origFilePath));
        String incomingXml = new String(Files.readAllBytes(incomingFilePath));
        newXml = XmlUtil.mergeXmlByPreservingField(origXml, incomingXml, preservedFields.getJobs());

        System.out.print(newXml);

        Document newDoc = XmlUtil.xmlToDocument(newXml);

        NodeList descriptionNode = newDoc.getElementsByTagName("description");
        String descriptionValue = descriptionNode.item(0).getFirstChild().getNodeValue();
        Assert.assertEquals("incoming config", descriptionValue);

        NodeList credentialNode = XmlUtil.findNodeByXpath(newDoc, SINGLE_SCM_GIT_CREDENTIAL_XPATH);
        String credentialValue = credentialNode.item(0).getFirstChild().getNodeValue();
        Assert.assertEquals("origin credential", credentialValue);

        NodeList urlNode = XmlUtil.findNodeByXpath(newDoc, SINGLE_SCM_GIT_URL_XPATH);
        String urlValue = urlNode.item(0).getFirstChild().getNodeValue();
        Assert.assertEquals("https://www.origin.com", urlValue);
    }

    @Test public void testMergeMutipleScmWithGitConfig() throws Exception {
        preservedFields = new PreservedFields();
        preservedFields.getJobs().add(MULTIPLE_SCM_GIT_CREDENTIAL_XPATH);
        preservedFields.getJobs().add(MULTIPLE_SCM_GIT_URL_XPATH);

        origFilePath = Paths.get("./src/test/resources", "job_scms_originConfig.xml");
        incomingFilePath = Paths.get("./src/test/resources", "job_scms_incomingConfig.xml");

        String origXml = new String(Files.readAllBytes(origFilePath));
        String incomingXml = new String(Files.readAllBytes(incomingFilePath));
        newXml = XmlUtil.mergeXmlByPreservingField(origXml, incomingXml, preservedFields.getJobs());

        System.out.print(newXml);

        Document newDoc = XmlUtil.xmlToDocument(newXml);

        NodeList descriptionNode = newDoc.getElementsByTagName("description");
        String descriptionValue = descriptionNode.item(0).getFirstChild().getNodeValue();
        Assert.assertEquals("incoming config", descriptionValue);

        NodeList credentialNode = XmlUtil.findNodeByXpath(newDoc, MULTIPLE_SCM_GIT_CREDENTIAL_XPATH);
        String credentialValue1 = credentialNode.item(0).getFirstChild().getNodeValue();
        Assert.assertEquals("origin credential", credentialValue1);
        String credentialValue2 = credentialNode.item(1).getFirstChild().getNodeValue();
        Assert.assertEquals("origin credential", credentialValue2);

        NodeList urlNode = XmlUtil.findNodeByXpath(newDoc, MULTIPLE_SCM_GIT_URL_XPATH);
        String urlValue1 = urlNode.item(0).getFirstChild().getNodeValue();
        Assert.assertEquals("https://www.origin.com/1/", urlValue1);
        String urlValue2 = urlNode.item(1).getFirstChild().getNodeValue();
        Assert.assertEquals("https://www.origin.com/2/", urlValue2);
    }
}
