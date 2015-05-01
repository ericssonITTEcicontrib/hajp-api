package com.ericsson.jenkinsci.hajp.api.files;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class TestZipUtil {

    @Rule public TemporaryFolder rule = new TemporaryFolder();

    private ZipUtil zipUtil;
    private File root;

    public TestZipUtil() throws IOException {
        zipUtil = new ZipUtil();
    }

    @Before
    public void before() throws IOException {
        root = rule.newFolder("root");
        createTestFiles();
    }

    @Test
    public void testZip() throws Exception {

        String zipFilename = "test.zip";
        zipUtil.zip(root.toPath(), root.toPath(), zipFilename);

        File zipFile = new File(root, zipFilename);
        Assert.assertTrue(zipFile.isFile());

        File dir = new File(root, "testDir");
        dir.mkdir();

        zipUtil.unzip(zipFile.toPath(), dir.toPath());

        assertTestFiles(dir);
    }

    private void createTestFiles() throws IOException {
        File file1 = new File(root, "file1.txt");
        file1.createNewFile();
        File file2 = new File(root, "file2.txt");
        file2.createNewFile();
        File dir1 = new File(root, "dir1");
        dir1.mkdir();
        File file3 = new File(dir1, "file3.txt");
        file3.createNewFile();
        File file4 = new File(dir1, "file4.txt");
        file4.createNewFile();
        File dir2 = new File(root, "dir2");
        dir2.mkdir();
        File file5 = new File(dir2, "file5.txt");
        file5.createNewFile();
        File file6 = new File(dir2, "file6.txt");
        file6.createNewFile();
    }

    private void assertTestFiles(File dir) throws IOException {
        File file1 = new File(dir, "file1.txt");
        Assert.assertTrue(file1.isFile());
        Assert.assertTrue(file1.exists());
        File file2 = new File(dir, "file2.txt");
        Assert.assertTrue(file2.isFile());
        Assert.assertTrue(file2.exists());
        File dir1 = new File(dir, "dir1");
        Assert.assertTrue(dir1.isDirectory());
        Assert.assertTrue(dir1.exists());
        File file3 = new File(dir1, "file3.txt");
        Assert.assertTrue(file3.isFile());
        Assert.assertTrue(file3.exists());
        File file4 = new File(dir1, "file4.txt");
        Assert.assertTrue(file4.isFile());
        Assert.assertTrue(file4.exists());
        File dir2 = new File(dir, "dir2");
        Assert.assertTrue(dir2.isDirectory());
        Assert.assertTrue(dir2.exists());
        File file5 = new File(dir2, "file5.txt");
        Assert.assertTrue(file5.isFile());
        Assert.assertTrue(file5.exists());
        File file6 = new File(dir2, "file6.txt");
        Assert.assertTrue(file6.isFile());
        Assert.assertTrue(file6.exists());
    }
}
