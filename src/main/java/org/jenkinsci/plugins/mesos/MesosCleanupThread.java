package org.jenkinsci.plugins.mesos;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.OfflineCause;
import jenkins.model.Jenkins;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 This file is part of the JCloud Jenkins Plugin. (https://github.com/jenkinsci/jclouds-plugin)
 commit id 20c6ca884abb172b27d0af82545c8915ce08f618.

 This file was modified to work with the Mesos Jenkins plugin and based off the JClouds Jenkins Plugin.

 According to the Jenkins Plugin guide (https://wiki.jenkins-ci.org/display/JENKINS/Before+starting+a+new+plugin),
 If no license is defined in the form of a header, LICENSE file, or in the license section, the code is assumed to be
 under The MIT License.

 The MIT License (MIT)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
@Extension
public class MesosCleanupThread extends AsyncPeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(MesosCleanupThread.class.getName());

    public MesosCleanupThread() {
        super("Mesos pending deletion slave cleanup");
    }

    @Override
    public long getRecurrencePeriod() {
        return MIN;
    }

    public static void invoke() {
        getInstance().run();
    }

    private static MesosCleanupThread getInstance() {
        return Jenkins.get().getExtensionList(AsyncPeriodicWork.class).get(MesosCleanupThread.class);
    }

    @Override
    protected void execute(TaskListener listener) {
        final ImmutableList.Builder<ListenableFuture<?>> deletedNodesBuilder = ImmutableList.builder();
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Computer.threadPoolForRemoting);

        int deleteCount = 0;

        for (final Computer c : Jenkins.get().getComputers()) {
          if (MesosComputer.class.isInstance(c)) {
            MesosSlave mesosSlave = (MesosSlave) c.getNode();
            if (mesosSlave != null && mesosSlave.isPendingDelete()) {
              final MesosComputer comp = (MesosComputer) c;
              LOGGER.log(Level.INFO, "Marked " + comp.getName() + " for deletion");
              if(comp.isIdle()) { //only delete it if it is really idle
                ListenableFuture<?> f = executor.submit(() -> {
                    LOGGER.log(Level.INFO, "Deleting pending node " + comp.getName());
                  if(comp.isOffline() && comp.getChannel() == null) {
                    //maybe slave was never online.. delete it from Jenkins instance
                    comp.deleteSlave();
                  } else {
                    //disconnect slave so the task at mesos can finish and dont get killed.
                    comp.disconnect(OfflineCause.create(Messages._deletedCause()));
                  }
                });
                deletedNodesBuilder.add(f);
                deleteCount++;
              }
            } else {
                LOGGER.log(Level.FINE, c.getName() + " with slave " + mesosSlave +
                      " is not pending deletion or the slave is null");
            }

          } else {
              LOGGER.log(Level.FINER, c.getName() + " is not a mesos computer, it is a " + c.getClass().getName());
          }
        }

        LOGGER.log(Level.INFO, "Deleted Nodes: " + deleteCount);
        Futures.getUnchecked(Futures.successfulAsList(deletedNodesBuilder.build()));
    }
}
