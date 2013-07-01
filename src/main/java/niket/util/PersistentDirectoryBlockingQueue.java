/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A live blocking queue which provides files from a given directory sorted by
 * File.lastModified().
 *
 * There is no method to put files in the queue. The files are put in the queue
 * only by physically placing a file in the given directory.
 *
 * It moves the files which have been taken off the queue to a subdirectory
 * named, "inProgress". On restart the files in "inProgress" are put back on the
 * queue. To avoid them from reappearing on restart, the caller is expected to
 * delete/move the taken file.
 *
 * Doesn't look for files in subdirectory.
 *
 * @author niket
 */
public class PersistentDirectoryBlockingQueue {

    private static final Logger logger = LoggerFactory.getLogger(PersistentDirectoryBlockingQueue.class);
    private static final Comparator<File> Comparator = new Comparator<File>() {
        @Override
        public int compare(File o1, File o2) {
            return (int) (o1.lastModified() - o2.lastModified());
        }
    };
    private final FileFilter wrappedFilter;
    private final File persistanceDirectory;
    private final SortedSet<File> sortedFiles;
    private File directory;

    public PersistentDirectoryBlockingQueue(File directory, final FileFilter fileFilter) {
        this.directory = directory;
        this.persistanceDirectory = new File(this.directory, "inProgress");
        if (!this.persistanceDirectory.exists()) {
            this.persistanceDirectory.mkdir();
        }
        this.sortedFiles = new TreeSet<>(Comparator);
        this.sortedFiles.addAll(listFiles(this.persistanceDirectory));
        this.wrappedFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isFile()
                        && f.canRead()
                        && (fileFilter == null ? true : fileFilter.accept(f));
            }
        };
        logger.info("Target dir: " + this.directory.getAbsolutePath()
                + ", Persistance Dir: " + this.persistanceDirectory.getAbsolutePath());
    }

    private List<File> listFiles(File dir) {
        return Arrays.asList(dir.listFiles(wrappedFilter));
    }

    /**
     * Switch target directory.
     *
     * @param directory
     */
    public void setDirectory(File directory) {
        synchronized (this) {
            this.directory = directory;
            this.sortedFiles.clear();
            logger.info("Updated target dir");
            logger.info("Target dir: " + this.directory.getAbsolutePath()
                    + ", Persistance Dir: " + this.persistanceDirectory.getAbsolutePath());
        }
    }

    /**
     * Blocks and waits for the next file.
     *
     * @return
     * @throws InterruptedException
     */
    public File take() throws IOException {
        synchronized (this) {
            logger.debug("Fetching next file");
            while (this.sortedFiles.isEmpty()) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                this.sortedFiles.addAll(listFiles(this.directory));
            }
            if (Thread.currentThread().isInterrupted()) {
                return null;
            } else {
                return takeNowItIsAvailable();
            }
        }
    }

    private File takeNowItIsAvailable() throws IOException {
        final File first = this.sortedFiles.first();
        final File file = Files.move(first.toPath(),
                new File(persistanceDirectory, first.getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE).toFile();
        this.sortedFiles.remove(first);
        return file;
    }
}
