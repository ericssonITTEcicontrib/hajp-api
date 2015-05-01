package com.ericsson.jenkinsci.hajp.api;

import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class) public class CredentialsManagerTest {

    @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Mock private static Jenkins mockJenkins;

    CredentialsManager unitUnderTest;

    private File rootDir;
    private File secretsDir;
    private File secretsTestContent;
    private File secretsFile;
    private File notSoSecretsFile;
    private File identitySecretFile;
    private File credentialsFile;

    //File contents
    private String fakeCredentials = "test-credentials";
    private String fakeSecretKey = "test-key";
    private String fakeNotSecretKey = "test-notkey";
    private String fakeIdentityKey = "test-identity-key";
    private String fakeSecretDirContent = "test-secretsfolder-key";

    @After public void tearDown() throws Exception {
        tmpFolder.delete();
    }

    @Before public void setUp() throws Exception {
        tmpFolder.create();
        //Prepare mocks
        rootDir = tmpFolder.getRoot();

        Mockito.when(mockJenkins.getRootDir()).thenReturn(rootDir);

        unitUnderTest = new CredentialsManager(mockJenkins);
        if (createSecretsDir() && createSecretsFiles() && createCredentialsFile()) {
            addCredentials();
            addSecretKey();
            addNotSoSecrets();
            addIdentitySecrets();
            addsecretsTestContent();
        } else {
            throw new Exception("Tmp Folder can not be prepared");
        }
    }

    @Test public void testPackUnpackSecretKeys() throws Exception {
        byte[] zipArr = unitUnderTest.packSecretKey();
        // First copy all files to be replaced
        File tmpSecretsFile = new File(rootDir + "/" + "fakeSecrets");
        File tmpNotSecretsFile = new File(rootDir + "/" + "fakeNotSecrets");
        File tmpIdentityFile = new File(rootDir + "/" + "fake-identity.key.enc");
        FileUtils.copyFile(secretsFile, tmpSecretsFile);
        FileUtils.copyFile(notSoSecretsFile, tmpNotSecretsFile);
        FileUtils.copyFile(identitySecretFile, tmpIdentityFile);

        //Unpack compressed versions
        unitUnderTest.unpackSecretKeys(zipArr);

        assertTrue(FileUtils.contentEquals(tmpSecretsFile, secretsFile));
        assertTrue(FileUtils.contentEquals(tmpNotSecretsFile, notSoSecretsFile));
        assertTrue(FileUtils.contentEquals(tmpIdentityFile, identitySecretFile));
    }

    @Test public void testUnpackSecretDir() throws Exception {
        byte[] zipArr = unitUnderTest.packSecretsDir();

        //First copy secrets dir to be replaced elsewhere
        File tmpSecretsDir = new File(rootDir + "/" + "secrets-fake");
        File tmpSecretsDirContent = new File(tmpSecretsDir + "/" + "test.txt");
        FileUtils.copyDirectory(secretsDir, tmpSecretsDir);

        unitUnderTest.unpackSecretDir(zipArr);

        assertTrue(FileUtils.contentEquals(secretsTestContent, tmpSecretsDirContent));
    }

    @Test public void testUnpackCredentials() throws Exception {
        byte[] origArr = FileUtils.readFileToByteArray(credentialsFile);
        unitUnderTest.unpackCredentials(origArr);
        assertTrue(Arrays.equals(origArr, FileUtils.readFileToByteArray(credentialsFile)));
    }

    @Test public void testRestartJenkins() throws Exception {
        unitUnderTest.restartJenkins();
        Mockito.verify(mockJenkins).restart();
    }

    @Test public void testPackSecretsDir() throws Exception {
        unitUnderTest.packSecretsDir();
    }

    @Test public void testPackSecretKey() throws Exception {
        unitUnderTest.packSecretKey();
    }

    @Test public void testPackCredentials() throws Exception {
        unitUnderTest.packCredentials();
    }

    @Test public void testCompareZip() throws Exception {
        byte[] credBytes = unitUnderTest.packCredentials();
        byte[] secretBytes = unitUnderTest.packSecretKey();

        byte[] rndZip = new byte[credBytes.length + secretBytes.length];
        System.arraycopy(credBytes, 0, rndZip, 0, credBytes.length);
        System.arraycopy(secretBytes, 0, rndZip, credBytes.length, secretBytes.length);
        assertTrue(!unitUnderTest.compareZip(credBytes, secretBytes));
        assertTrue(!unitUnderTest.compareZip(credBytes, rndZip));
        assertTrue(!unitUnderTest.compareZip(credBytes, null));
    }

    @Test public void testCompareCredentials() throws Exception {
        byte[] credBytes = unitUnderTest.packCredentials();
        byte[] testBytes = "Test string".getBytes();

        assertTrue(unitUnderTest.compareCredentials(credBytes));
        assertTrue(!unitUnderTest.compareCredentials(testBytes));
        assertTrue(!unitUnderTest.compareCredentials(null));
    }

    @Test public void testByteArrayComp() throws Exception {
        byte[] myvar1 = "Test string".getBytes();
        byte[] myvar2 = "Test string".getBytes();
        byte[] myvar3 = "test string".getBytes();

        assertTrue(CredentialsManager.byteArrayComp(myvar1, myvar2));
        assertTrue(!CredentialsManager.byteArrayComp(myvar1, null));
        assertTrue(!CredentialsManager.byteArrayComp(myvar1, myvar3));
    }

    private boolean createSecretsDir() throws IOException {
        secretsDir = tmpFolder.newFolder("secrets");
        secretsTestContent = new File(secretsDir.getAbsolutePath() + "/" + "test.txt");
        return secretsTestContent.createNewFile();
    }

    private boolean createSecretsFiles() throws IOException {
        secretsFile = tmpFolder.newFile("secret.key");
        notSoSecretsFile = tmpFolder.newFile("secret.key.not-so-secret");
        identitySecretFile = tmpFolder.newFile("identity.key.enc");
        return true;
    }

    private boolean createCredentialsFile() throws IOException {
        credentialsFile = tmpFolder.newFile("credentials.xml");
        return true;
    }

    private void addCredentials() throws IOException {
        FileUtils.writeStringToFile(credentialsFile, fakeCredentials);
    }

    private void addSecretKey() throws IOException {
        FileUtils.writeStringToFile(secretsFile, fakeSecretKey);
    }

    private void addNotSoSecrets() throws IOException {
        FileUtils.writeStringToFile(notSoSecretsFile, fakeNotSecretKey);
    }

    private void addIdentitySecrets() throws IOException {
        FileUtils.writeStringToFile(identitySecretFile, fakeIdentityKey);
    }

    private void addsecretsTestContent() throws IOException {
        FileUtils.writeStringToFile(secretsTestContent, fakeSecretDirContent);
    }


}
