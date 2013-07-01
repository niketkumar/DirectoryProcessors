/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package example;

import example.states.FailedState3;
import example.states.FailedState2;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import niket.dirprocs.State;
import niket.dirprocs.core.StateManager;
import example.states.State3;
import example.states.State1;
import example.states.State2;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author niket
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("log4j.defaultInitOverride", String.valueOf(true));
        final URL log4jProps = Loader.getResource("example/log4jDirectoryProcessors.properties");
        PropertyConfigurator.configure(log4jProps);
        final Logger logger = LoggerFactory.getLogger(Main.class);
        logger.info("Log4j Config loaded from " + log4jProps.toString());

        final StateManager stateManager = new StateManager();

        final State1 state1 =
                new State1(new File("C:\\Users\\niket\\dirprocs\\state1"));
        final FailedState2 failedState2 =
                new FailedState2(new File("C:\\Users\\niket\\dirprocs\\failedState2"));
        final State2 state2 =
                new State2(new File("C:\\Users\\niket\\dirprocs\\state2"));
        final State3 state3 =
                new State3(new File("C:\\Users\\niket\\dirprocs\\state3"));
        final FailedState3 failedState3 =
                new FailedState3(new File("C:\\Users\\niket\\dirprocs\\failedState3"));

        final State1Processor processor1 = new State1Processor();
        final State2Processor processor2 = new State2Processor();

        stateManager.addDefinition(state1, processor1,
                new HashSet<>(Arrays.asList(new State[]{failedState2, state2})));
        stateManager.addDefinition(state2, processor2,
                new HashSet<>(Arrays.asList(new State[]{state3, failedState3})));

        final LoggerObserver logger1 = new LoggerObserver();
        stateManager.addObserver(state1, state2, logger1);
        stateManager.addObserver(state1, failedState2, logger1);
        stateManager.addObserver(state2, state3, logger1);
        stateManager.addObserver(state2, failedState3, logger1);

        final UIObserver uiObserver = new UIObserver();
        stateManager.addObserver(state1, state2, uiObserver);
        stateManager.addObserver(state1, failedState2, uiObserver);
        stateManager.addObserver(state2, state3, uiObserver);
        stateManager.addObserver(state2, failedState3, uiObserver);

        stateManager.addProcessorThreads(processor1, 1);
        stateManager.addProcessorThreads(processor2, 1);

        stateManager.startAll();

        TimeUnit.SECONDS.sleep(5);

        stateManager.stopProcessor(State1Processor.class);

        TimeUnit.SECONDS.sleep(5);

        stateManager.startProcessor(State1Processor.class);

        TimeUnit.SECONDS.sleep(5);

        stateManager.updateState(new State1(new File("C:\\Users\\niket\\dirprocs\\state4")));

        TimeUnit.SECONDS.sleep(5);

        stateManager.disableProcessor(State2Processor.class);

        TimeUnit.SECONDS.sleep(5);

        stateManager.enableProcessor(State2Processor.class, 3);

        TimeUnit.SECONDS.sleep(30);

        stateManager.shutdown();
    }
}
