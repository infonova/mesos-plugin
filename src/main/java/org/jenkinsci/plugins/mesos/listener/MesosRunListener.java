package org.jenkinsci.plugins.mesos.listener;

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import org.jenkinsci.plugins.mesos.JenkinsScheduler;
import org.jenkinsci.plugins.mesos.Mesos;
import org.jenkinsci.plugins.mesos.MesosSlave;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@SuppressWarnings("rawtypes")
@Extension
public class MesosRunListener extends RunListener<Run> {

  private static final Logger LOGGER = Logger.getLogger(MesosRunListener.class.getName());
  private static final String EXCLUDED_CLASSES_FROM_LOG_OUTPUT_REGEX = "hudson\\.maven\\.MavenBuild";

  public MesosRunListener() {

  }

  /**
   * @param targetType
   */
  @SuppressWarnings("unchecked")
  public MesosRunListener(Class targetType) {
    super(targetType);
  }

  /**
   * Prints the actual Hostname where Mesos slave is provisioned in console output.
   * This would help us debug/take action if build fails in that slave.
   */
  @Override
  public void onStarted(Run r, TaskListener listener) {
    if (!skipLogfileOutputForRun(r)) {
      Node node = getCurrentNode();
      if (node instanceof MesosSlave) {
        try {
          MesosSlave mesosSlave = (MesosSlave) node;
          JenkinsScheduler jenkinsScheduler = (JenkinsScheduler) Mesos.getInstance(mesosSlave.getCloud()).getScheduler();

          String mesosNodeHostname = jenkinsScheduler.getResult(mesosSlave.getDisplayName()).getSlave().getHostName();
          String hostname = node.toComputer().getHostName();

          String msg = String.format("This build is running on %s", mesosNodeHostname);
          String containerID = mesosSlave.getDockerContainerID();

          if (containerID != null) {
            msg += String.format(" in %s", containerID);
          }

          PrintStream logger = listener.getLogger();
          logger.println();
          logger.println(msg);

          if (hostname != null) {
            logger.println("Jenkins Slave (hostname): " + hostname);
          }
          logger.println();

        } catch (IOException e) {
          LOGGER.warning("IOException while trying to get hostname: " + e);
          e.printStackTrace();
        } catch (InterruptedException e) {
          LOGGER.warning("InterruptedException while trying to get hostname: " + e);
        }
      }
    }
  }

  @Override
  public void onCompleted(Run run, TaskListener listener) {
    if (!skipLogfileOutputForRun(run)) {
      Node node = getCurrentNode();
      if (node instanceof MesosSlave) {
        MesosSlave mesosSlave = (MesosSlave) node;
        String monitoringUrl = mesosSlave.getMonitoringURL();
        PrintStream logger = listener.getLogger();

        logger.println();
        if(monitoringUrl != null) {
          logger.println("Slave resource usage: " + monitoringUrl);
        } else {
          logger.println("Slave resource usage is not available for this build.");
        }
        logger.println();
      }
    }
  }

  private boolean skipLogfileOutputForRun(Run r) {
    return r == null || Pattern.matches(EXCLUDED_CLASSES_FROM_LOG_OUTPUT_REGEX,r.getClass().getName());
  }

  /**
   * Returns the current {@link Node} on which we are building.
   */
  private final Node getCurrentNode() {
    return Executor.currentExecutor().getOwner().getNode();
  }

}
