package com.ericsson.jenkinsci.hajp.api.files;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class consists of instance methods to zip and unzip files.
 */
public class ZipUtil {
    public static final String ZIP_ARCHIVER = "zip";
    public static final String ZIP_SUFFIX = ".zip";

    public static final String ARCHIVE_FILENAME = "archive";

    /**
     * Unzip files and store them in the specified directory.
     * @param zipFile the zip file
     * @param destDir the destination directory
     * @throws Exception if failed to unzip files
     */
    public void unzip(final Path zipFile, final Path destDir) throws Exception {
        if (!Files.exists(destDir)) {
            Files.createDirectory(destDir);
        }

        final InputStream is = new FileInputStream(zipFile.toFile());
        final ArchiveInputStream ain =
            new ArchiveStreamFactory().createArchiveInputStream(ZIP_ARCHIVER, is);

        ArchiveEntry entry = ain.getNextEntry();
        while (entry != null) {
            unzipEntry(destDir.toFile(), entry, ain);
            entry = ain.getNextEntry();
        }
        ain.close();
        is.close();
    }

    private void unzipEntry(final File destDir, final ArchiveEntry entry, final ArchiveInputStream ain) throws Exception {
        File archiveEntry = new File(destDir, entry.getName());
        if (entry.isDirectory()) {
            archiveEntry.mkdir();
        } else {
            OutputStream out = new FileOutputStream(archiveEntry);
            try {
                IOUtils.copy(ain, out);
            } finally {
                out.close();
            }
        }
    }

    /**
     * Zip all files under the directory
     * @param srcDir the parent directory of files to be zipped
     * @param dir the directory where the zip file will be created
     * @param zipFilename the zip file name
     * @throws Exception if failed to zip files
     */
    public void zip(final Path srcDir, final Path dir, final String zipFilename) throws Exception {
        final Path zipFile = dir.resolve(zipFilename);
        final OutputStream os = new FileOutputStream(zipFile.toFile());
        final ArchiveOutputStream aos =
            new ArchiveStreamFactory().createArchiveOutputStream(ZIP_ARCHIVER, os);
        try {
            for (File file : srcDir.toFile().listFiles()) {
                if (!file.getName().equals(zipFilename)) {
                    zipFile(file, "", aos);
                }
            }
        } finally {
            aos.close();
        }
    }

    private void zipFile(final File file, final String parentFolder, final ArchiveOutputStream aos)
        throws IOException {
        // If it is a directory and not named archive, process it.
        if (!file.isFile() && !file.getName().equals(ARCHIVE_FILENAME)) {
            aos.putArchiveEntry(new ZipArchiveEntry(file.getName() + File.separator));
            aos.closeArchiveEntry();
            for (File f : file.listFiles()) {
                zipFile(f, parentFolder + File.separator + file.getName() + File.separator, aos);
            }
        } else if(file.isFile()) {
            aos.putArchiveEntry(new ZipArchiveEntry(parentFolder + file.getName()));
            IOUtils.copy(new FileInputStream(file), aos);
            aos.closeArchiveEntry();
        }
    }
}
