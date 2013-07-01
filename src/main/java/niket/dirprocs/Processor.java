/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.dirprocs;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * @author niket
 */
public interface Processor {

    ProcessedState execute(File file);

    FileFilter getFileFilter();
}
