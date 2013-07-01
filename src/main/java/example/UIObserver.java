/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package example;

import java.util.Observable;
import java.util.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author niket
 */
public class UIObserver implements Observer {

    private static final Logger logger = LoggerFactory.getLogger(UIObserver.class);

    @Override
    public void update(Observable o, Object arg) {
        logger.info(arg.toString());
    }
}
