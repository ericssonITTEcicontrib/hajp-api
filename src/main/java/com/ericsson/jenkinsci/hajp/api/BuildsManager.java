package com.ericsson.jenkinsci.hajp.api;

import com.ericsson.jenkins.hajp.api.Messages;
import com.ericsson.jenkinsci.hajp.api.files.ZipUtil;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.FreeStyleBuild;
import hudson.model.RunAction;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.RunList;
import jenkins.model.PeepholePermalink;
import jenkins.model.RunAction2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class consists of instance methods that will be utilized by HAJP-CORE
 * and features to access and modify internal Jenkins core builds related
 * functionalities.
 */
public class BuildsManager {
    public static final String BUILD_TAG = "_b";
    public static final String BUILD_CLASHING = ".clashing";
    public static final String BUILD_TMP = ".tmp";
    public static final String BUILDS_DIRNAME = "builds";

    public static final String LAST_FAILED_BUILD = "lastFailedBuild";
    public static final String LAST_STABLE_BUILD = "lastStableBuild";
    public static final String LAST_SUCCESSFUL_BUILD = "lastSuccessfulBuild";
    public static final String LAST_UNSTABLE_BUILD = "lastUnstableBuild";
    public static final String LAST_UNSUCCESSFUL_BUILD = "lastUnsuccessfulBuild";

    private JobsManager jobsManager;
    private ZipUtil zipUtil;

    /**
     * Constructor.
     *
     * @param jobsManager the jobsManager
     */
    public BuildsManager(JobsManager jobsManager) {
        this.jobsManager = jobsManager;
        this.zipUtil = new ZipUtil();
    }

    /**
     * @return the path to builds directory
     * @throws BuildsManagementException if failed to find the builds directory
     */
    public Path getBuildsDir(String jobName) throws BuildsManagementException {
        Path jobDir = jobsManager.getJobDir(jobName).toPath();
        Path buildsDir = jobsManager.getJobDir(jobName).toPath().resolve(BUILDS_DIRNAME);
        if (!Files.exists(jobDir)) {
            throw new BuildsManagementException(Messages.jobs_job_dir_does_not_exist(jobName),
                jobName);
        }
        try {
            if (!Files.exists(buildsDir)) {
                Files.createDirectory(buildsDir);
            }
        } catch (IOException e) {
            throw new BuildsManagementException(Messages.builds_dir_create_error(jobName), jobName);
        }
        return buildsDir;
    }

    /**
     * @param jobName     the job name
     * @param buildNumber the build number
     * @return the path to number build directory
     * @throws BuildsManagementException if failed to find the build directory
     */
    public Path getNumberBuildDir(String jobName, int buildNumber)
        throws BuildsManagementException {
        Path buildNumberDir = getBuildsDir(jobName).resolve("" + buildNumber);
        return buildNumberDir;
    }

    /**
     * @param jobName     the job name
     * @param buildNumber the build number
     * @return the name of the timestamp build directory
     * @throws BuildsManagementException if failed to find the build directory
     */
    public String getTimestampBuildDirName(String jobName, int buildNumber)
        throws BuildsManagementException {
        Path buildNumberDir = getNumberBuildDir(jobName, buildNumber);
        try {
            return Files.readSymbolicLink(buildNumberDir).getFileName().toString();
        } catch (IOException e) {
            throw new BuildsManagementException(
                Messages.builds_build_dir_name_error(jobName, buildNumber), e, jobName,
                buildNumber);
        }
    }

    /**
     * Grab the build as a zip file and store it under the builds directory.
     *
     * @param jobName     the job name
     * @param buildNumber the build number
     * @return a zip file as byte array
     * @throws BuildsManagementException if failed to
     */
    public byte[] grabBuild(String jobName, int buildNumber) throws BuildsManagementException {
        String zipFileName = getZipFileName(jobName, buildNumber);
        Path buildsDir = getBuildsDir(jobName);
        Path numberBuildDir = getNumberBuildDir(jobName, buildNumber);
        try {
            Path timestampBuildDir = Files.readSymbolicLink(numberBuildDir);
            Path buildDir = buildsDir.resolve(timestampBuildDir);
            zipUtil.zip(buildDir, buildsDir, zipFileName);
            Path zipFile = getZipFile(buildsDir, jobName, buildNumber);
            byte[] fileAsByteArray = Files.readAllBytes(zipFile);
            Files.delete(zipFile);
            return fileAsByteArray;
        } catch (Exception e) {
            throw new BuildsManagementException(
                Messages.builds_build_zip_error(jobName, buildNumber), e, jobName, buildNumber);
        }
    }

    /**
     * Extract the build from a zip file to the builds directory.
     *
     * @param zipFileAsByteArray the zip file as byte array
     * @param jobName            the job name
     * @param buildNumber        the build number
     * @return the path to the directory where is extracted build is located
     * @throws BuildsManagementException if failed to
     */
    public Path extractBuild(byte[] zipFileAsByteArray, String jobName, int buildNumber,
        String buildDirName) throws BuildsManagementException {
        Path destDir = getBuildsDir(jobName);
        return extractBuild(destDir, zipFileAsByteArray, jobName, buildNumber, buildDirName);
    }

    /**
     * Extract the build from a zip file to the specified destination directory, which should be
     * under builds directory. This operation will be done in 4 steps:
     * <lu>1. unzip the content to a tmp directory to make sure this is no issue.</lu>
     * <lu>2. If it happens that the current build already exists locally, then save the existing one to a clashing directory. </lu>
     * <lu>3. move the unzip contain to the destination directory (builds).</lu>
     * <lu>4. create a build number link to the build directory named with a timestamp.</lu>
     *
     * @param destDir            the path to the destination directory under which files will be extracted from the zip file
     * @param zipFileAsByteArray the zip file as byte array
     * @param jobName            the job name
     * @param buildNumber        the build number
     * @param buildDirName       the build directory name
     * @return the path to the directory where is extracted build is located
     * @throws BuildsManagementException if failed to
     */
    public Path extractBuild(Path destDir, byte[] zipFileAsByteArray, String jobName,
        int buildNumber, String buildDirName) throws BuildsManagementException {
        Path buildNumberDir = destDir.resolve("" + buildNumber);
        Path zipFile = getZipFile(destDir, jobName, buildNumber);
        try {
            Path tmpBuildDir = createTmpBuildDir(destDir, buildNumber);
            unzipToTmpDir(zipFileAsByteArray, zipFile, tmpBuildDir);
            saveClashingBuild(buildNumberDir, destDir, buildDirName);
            Path destBuildDir = moveToDestDir(tmpBuildDir, destDir, buildDirName);
            Files.createSymbolicLink(buildNumberDir, destBuildDir);
            return destBuildDir;
        } catch (Exception e) {
            throw new BuildsManagementException(
                Messages.builds_build_unzip_error(jobName, buildNumber), e, jobName, buildNumber);
        }
    }

    private Path moveToDestDir(Path tmpBuildDir, Path destDir, String buildDirName)
        throws IOException {
        Path destBuildDir = destDir.resolve(buildDirName);
        if (!Files.exists(destBuildDir)) {
            Files.move(tmpBuildDir, destBuildDir);
        }
        Files.delete(destDir.resolve(BUILD_TMP));
        return destBuildDir;
    }

    private void saveClashingBuild(Path buildDir, Path destDir, String buildDirName)
        throws IOException {
        Path clashingDir = destDir.resolve(BUILD_CLASHING);
        if (Files.exists(buildDir)) {
            if (!Files.exists(clashingDir)) {
                Files.createDirectory(clashingDir);
            }
            Path clashingBuildDir = clashingDir.resolve(buildDirName);
            if (!Files.exists(clashingBuildDir)) {
                Files.move(buildDir, clashingBuildDir);
            }
        }
    }

    private void unzipToTmpDir(byte[] zipFileAsByteArray, Path zipFile, Path tmpBuildDir)
        throws Exception {
        Files.createFile(zipFile);
        Files.write(zipFile, zipFileAsByteArray);
        zipUtil.unzip(zipFile, tmpBuildDir);
        Files.delete(zipFile);
    }

    private Path createTmpBuildDir(Path destDir, int buildNumber) throws IOException {
        Path tmpBuildsDir = destDir.resolve(BUILD_TMP);
        Path tmpBuildNumberDir = tmpBuildsDir.resolve("" + buildNumber);

        if (!Files.exists(tmpBuildsDir)) {
            Files.createDirectory(tmpBuildsDir);
        }
        if (Files.exists(tmpBuildNumberDir)) {
            Files.delete(tmpBuildNumberDir);
        }
        Files.createDirectory(tmpBuildNumberDir);

        return tmpBuildNumberDir;
    }

    /**
     * @param buildsDir   the builds directory
     * @param jobName     the job name
     * @param buildNumber the build number
     * @return the path to the zip file
     * @throws BuildsManagementException if failed to find the zip file
     */
    private Path getZipFile(Path buildsDir, String jobName, int buildNumber)
        throws BuildsManagementException {
        String zipFileName = getZipFileName(jobName, buildNumber);
        return buildsDir.resolve(zipFileName);
    }

    /**
     * @param jobName     the job name
     * @param buildNumber the build number
     * @return the build zip file name
     */
    private String getZipFileName(String jobName, int buildNumber) {
        return jobName + BUILD_TAG + buildNumber + zipUtil.ZIP_SUFFIX;
    }

    /**
     * @param project the project
     * @return the list of builds under the project
     */
    public RunList listBuilds(AbstractProject project) {
        return project.getBuilds();
    }

    /**
     * @param jobName     the jobName
     * @param buildNumber the build number
     * @return the build
     */
    public Run getBuild(String jobName, int buildNumber) throws BuildsManagementException {
        try {
            Job<?, ?> job = jobsManager.getJob(jobName);
            Run build = job.getBuildByNumber(buildNumber);
            return build;
        } catch (JobManagementException e) {
            throw new BuildsManagementException(
                Messages.builds_build_not_found(jobName, buildNumber), e, jobName, buildNumber);
        }
    }

    /**
     * @param jobName     the jobName
     * @param buildNumber the build number
     * @return true if the build exists, false otherwise
     */
    public boolean buildExists(String jobName, int buildNumber) {
        try {
            return (getBuild(jobName, buildNumber) != null);
        } catch (BuildsManagementException e) {
            return false;
        }
    }

    /**
     * Create a new build and then dynamically add a it to its parent job.
     *
     * @param jobName     the job name
     * @param buildFolder the path to the build folder
     * @return the created build
     * @throws BuildsManagementException if failed to create the build
     * @throws InterruptedException if interrupted.
     * @throws IOException if io error occurs.
     * @throws JobManagementException if it occurs
     */
    public FreeStyleBuild createFreeStyleBuild(String jobName, Path buildFolder)
        throws BuildsManagementException, IOException, InterruptedException, JobManagementException {

        FreeStyleBuild build = null;
        FreeStyleProject project;
        project = jobsManager.getFreeStyleProject(jobName);
        build = new FreeStyleBuild(project, buildFolder.toFile());

        performBuildMaintenance(project, build);

        return build;
    }

    private void performBuildMaintenance(AbstractProject<FreeStyleProject, FreeStyleBuild> project,
        FreeStyleBuild build) throws BuildsManagementException, IOException, InterruptedException {

      PeepholePermalink.RunListenerImpl
            .fireCompleted(project.getBuildByNumber(build.getNumber()), TaskListener.NULL);

      for (Action a : build.getAllActions()) {
        if (a instanceof RunAction2) {
            ((RunAction2) a).onLoad(build);
        } else if (a instanceof RunAction) {
            ((RunAction) a).onLoad();
        }
      }

      if (build.getArtifactManager() != null) {
        build.getArtifactManager().onLoad(build);
      }

      project.assignBuildNumber();
      build.updateSymlinks(TaskListener.NULL);

    }

    /**
     * @param jobName     the job name
     * @param buildNumber the build number
     * @throws BuildsManagementException if failed to delete the build
     */
    public void deleteFreeStyleBuild(String jobName, int buildNumber)
        throws BuildsManagementException {
        try {
            FreeStyleProject project = jobsManager.getFreeStyleProject(jobName);
            Run build = project.getBuildByNumber(buildNumber);
            build.delete();
            project.getBuilds().remove(build);
        } catch (IOException | JobManagementException e) {
            throw new BuildsManagementException(
                Messages.builds_freestyle_delete_error(jobName, buildNumber), e, jobName,
                buildNumber);
        }
    }
}
