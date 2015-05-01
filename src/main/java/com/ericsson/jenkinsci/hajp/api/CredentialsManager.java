package com.ericsson.jenkinsci.hajp.api;


import hudson.lifecycle.RestartNotSupportedException;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jvnet.hudson.reactor.ReactorException;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class consists of instance methods that will be utilized by HAJP-CORE
 * and features to access and modify internal Jenkins core credentials and secrets/identities
 * related functionalities.
 */
@Log4j2 public class CredentialsManager {
    private static Random rand = new Random();

    @Getter private Jenkins jenkins;

    private File jenkinsLocation;
    private File jenkinsSecretsDir;
    private File secretKeysOutLoc;
    private File secretDirOutLoc;
    private File jenkinsSecretKeyLoc;
    private File jenkinsNotSoSecretKeyLoc;
    private File jenkinsIdentityKeyLoc;

    /**
     * Default constructor
     */
    public CredentialsManager(Jenkins jenkins) {
        this.jenkins = jenkins;
        this.jenkinsLocation = jenkins.getRootDir().getAbsoluteFile();
        this.secretDirOutLoc =
            new File(Objects.toString(jenkinsLocation + "/" + rand.nextLong()) + ".zip");
        this.secretKeysOutLoc =
            new File(Objects.toString(jenkinsLocation + "/secretkeys-" + rand.nextLong()) + ".zip");
        this.jenkinsSecretsDir = new File(jenkinsLocation.getAbsolutePath() + "/secrets");
        this.jenkinsSecretKeyLoc = new File(jenkinsLocation.getAbsolutePath() + "/secret.key");
        this.jenkinsNotSoSecretKeyLoc =
            new File(jenkinsLocation.getAbsolutePath() + "/secret.key.not-so-secret");
        this.jenkinsIdentityKeyLoc =
            new File(jenkinsLocation.getAbsolutePath() + "/identity.key.enc");
    }

    /**
     * Compares two byte arrays using array comparison
     *
     * @param arr1 byteArray one
     * @param arr2 byteArray two
     * @return comparison result
     */
    public static boolean byteArrayComp(byte[] arr1, byte[] arr2) {
        return Arrays.equals(arr1, arr2);
    }

    /**
     * Compares credentials file to Jenkins persisted copy
     *
     * @param credFileReceived Credential file bytes
     * @return comparison result
     */
    public boolean compareCredentials(byte[] credFileReceived) {
        try {
            return fileContentCompare(credFileReceived, packCredentials());
        } catch (IOException | ReactorException | InterruptedException ex) {
            log.error(ExceptionUtils.getStackTrace(ex));
        }
        return false;
    }

    /**
     * Compare zip files from content
     *
     * @param zip1 zip file as byte array
     * @param zip2 zip file as byte array
     * @return comparison result
     * @throws IOException
     */
    public boolean compareZip(byte[] zip1, byte[] zip2) throws IOException {
        boolean comparison = false;
        if (zip1 != null && zip2 != null) {
            File tmpFile1 =
                new File(Objects.toString(jenkinsLocation + "/" + rand.nextLong() + ".zip"));
            FileUtils.writeByteArrayToFile(tmpFile1, zip1);

            File tmpFile2 =
                new File(Objects.toString(jenkinsLocation + "/" + rand.nextLong() + ".zip"));
            FileUtils.writeByteArrayToFile(tmpFile2, zip2);

            comparison = ZipUtil.archiveEquals(tmpFile1, tmpFile2);

            FileUtils.deleteQuietly(tmpFile1);
            FileUtils.deleteQuietly(tmpFile2);
        }

        return comparison;
    }

    /**
     * Packages credentials into byte array
     * @return byte array
     * @throws IOException
     * @throws ReactorException
     * @throws InterruptedException
     */
    public byte[] packCredentials() throws IOException, ReactorException, InterruptedException {
        File tmpFile = new File(Objects.toString(jenkinsLocation + "/" + "credentials.xml"));
        return FileUtils.readFileToByteArray(tmpFile);
    }

    /**
     * Zip Secretkeys and Identity key to byte array
     *
     * @return zip as byte array
     */
    public byte[] packSecretKey() throws IOException {
        byte[] returnArr;
        List<File> fileArray = new ArrayList<File>();
        File[] fileArr = new File[fileArray.size()];
        fileArray.add(jenkinsSecretKeyLoc);
        fileArray.add(jenkinsNotSoSecretKeyLoc);
        fileArray.add(jenkinsIdentityKeyLoc);
        ZipUtil.packEntries(fileArray.toArray(fileArr), secretKeysOutLoc);
        returnArr = FileUtils.readFileToByteArray(secretKeysOutLoc);
        FileUtils.deleteQuietly(secretKeysOutLoc);
        return returnArr;
    }

    /**
     * Zip secrets folder on Jenkins home to byte array
     *
     * @return zip as byte array
     */
    public byte[] packSecretsDir() throws IOException {
        ZipUtil.pack(jenkinsSecretsDir, secretDirOutLoc);
        byte[] returnArr = FileUtils.readFileToByteArray(secretDirOutLoc);
        FileUtils.deleteQuietly(secretDirOutLoc);
        return returnArr;
    }

    /**
     * Instance level restart jenkins
     *
     * @throws RestartNotSupportedException
     */
    public void restartJenkins() throws RestartNotSupportedException {
        jenkins.restart();
    }

    /**
     * Saves credentialsFile from byte array to Jenkins home
     *
     * @param credentialsFile byte contents of credentials file
     * @throws IOException
     * @throws ReactorException
     * @throws InterruptedException
     */
    public void unpackCredentials(byte[] credentialsFile)
        throws IOException, ReactorException, InterruptedException {
        File tmpFile = new File(Objects.toString(jenkinsLocation + "/" + "credentials.xml"));
        FileUtils.deleteQuietly(tmpFile);
        FileUtils.writeByteArrayToFile(tmpFile, credentialsFile);
    }

    /**
     * Saves secrets directory to Jenkins home from zip
     *
     * @param compSecretDir zip byte array
     * @throws IOException
     */
    public void unpackSecretDir(byte[] compSecretDir) throws IOException {
        File tmpFile = new File(Objects.toString(jenkinsLocation + "/" + rand.nextLong() + ".zip"));
        File secretDir = new File(jenkinsLocation + "/secrets");
        FileUtils.writeByteArrayToFile(tmpFile, compSecretDir);
        FileUtils.deleteDirectory(secretDir);
        ZipUtil.unpack(tmpFile, secretDir);
        FileUtils.deleteQuietly(tmpFile);
    }

    /**
     * Saves secret and identity key files to Jenkins home from zip
     *
     * @param compSecretKeys zip byte array
     * @throws IOException
     */
    public void unpackSecretKeys(byte[] compSecretKeys) throws IOException {
        File tmpFile = new File(Objects.toString(jenkinsLocation + "/" + rand.nextLong() + ".zip"));
        FileUtils.writeByteArrayToFile(tmpFile, compSecretKeys);
        ZipUtil.unpackEntry(tmpFile, "secret.key", jenkinsSecretKeyLoc);
        ZipUtil.unpackEntry(tmpFile, "identity.key.enc", jenkinsIdentityKeyLoc);
        ZipUtil.unpackEntry(tmpFile, "secret.key.not-so-secret", jenkinsNotSoSecretKeyLoc);
        FileUtils.deleteQuietly(tmpFile);
    }


    private boolean fileContentCompare(byte[] arr1, byte[] arr2) throws IOException {
        return arr1 != null && arr2 != null & DigestUtils.md5Hex(arr1)
            .equals(DigestUtils.md5Hex(arr2));

    }
}
