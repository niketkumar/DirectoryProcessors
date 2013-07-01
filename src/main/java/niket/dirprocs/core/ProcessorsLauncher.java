/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.dirprocs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import niket.dirprocs.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author niket
 */
class ProcessorsLauncher {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorsLauncher.class);
    private final ConcurrentMap<Class<? extends Processor>, Integer> processorThreadCounts;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ExecutorService threadPool;
    private final ConcurrentMap<Class<? extends Processor>, List<Future>> futures;

    ProcessorsLauncher() {
        this.processorThreadCounts = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.futures = new ConcurrentHashMap<>();
    }

    void addProcessorThreads(Class<? extends Processor> processorClass, int nosThreads) {
        final Integer old = processorThreadCounts.put(processorClass, nosThreads);
        if (old == null) {
            logger.info(processorClass.getName()
                    + " will run in " + nosThreads + " threads");
        } else {
            logger.warn("Thread count for Processor "
                    + processorClass.getName() + " changed from "
                    + old + " to " + nosThreads);
        }
    }

    int threadCounts(Processor processor) {
        final Integer count = processorThreadCounts.get(processor.getClass());
        return null == count ? 1 : count;
    }

    boolean startAll(StateDefinitions definitions,
            StateTransitions transitions) {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("Launching all processors...");
            try {
                List<RunnableProcessor> runnables =
                        definitions.createRunnableProcessors(this, transitions);
                for (RunnableProcessor runnableProcessor : runnables) {
                    final Class<? extends Processor> processorClass =
                            runnableProcessor.getProcessorClass();
                    if (futures.containsKey(processorClass)) {
                        futures.get(processorClass).add(threadPool.submit(runnableProcessor));
                    } else {
                        final ArrayList<Future> list = new ArrayList<>();
                        list.add(threadPool.submit(runnableProcessor));
                        futures.put(processorClass, list);
                    }
                }
                return true;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                stopAll();
                return false;
            }
        } else {
            logger.warn("ProcessorLauncher is already running");
            return false;
        }
    }

    synchronized boolean stopAll() {
        if (isRunning.get()) {
            logger.warn("Stopping all processors...");
            for (List<Future> fs : futures.values()) {
                for (Future f : fs) {
                    while (!f.isDone()) {
                        f.cancel(true);
                    }
                }
            }
            futures.clear();
            isRunning.set(false);
            return true;
        } else {
            return false;
        }
    }

    synchronized boolean shutdown(StateTransitions transitions) {
        if (isRunning.get()) {
            logger.warn("Shutting down...");
            transitions.removeAllObservers();
            stopAll();
            if (!threadPool.isTerminated()) {
                threadPool.shutdownNow();
            }
            return true;
        } else {
            return false;
        }
    }

    synchronized boolean stopProcessor(Class<? extends Processor> processorClass) {
        if (isRunning.get()) {
            logger.warn("Stopping Processor " + processorClass.getName() + "...");
            final List<Future> fs = futures.remove(processorClass);
            if (null != fs) {
                for (Future f : fs) {
                    while (!f.isDone()) {
                        f.cancel(true);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    synchronized boolean startProcessor(Class<? extends Processor> processorClass,
            StateDefinitions definitions,
            StateTransitions transitions) {
        logger.warn("Starting Processor " + processorClass.getName() + "...");
        List<RunnableProcessor> runnables = definitions.createRunnableProcessors(
                processorClass,
                this,
                transitions);
        for (RunnableProcessor runnableProcessor : runnables) {
            if (futures.containsKey(processorClass)) {
                futures.get(processorClass).add(threadPool.submit(runnableProcessor));
            } else {
                final ArrayList<Future> list = new ArrayList<>();
                list.add(threadPool.submit(runnableProcessor));
                futures.put(processorClass, list);
            }
        }
        return true;

    }
}
