package org.jenkinsci.plugins.mesos.config.slavedefinitions;

import hudson.model.Node.Mode;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.mesos.Protos.ContainerInfo.DockerInfo.Network;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.*;
import java.util.logging.Logger;

@ExportedBean
public class MesosSlaveInfo {

  private static final String DEFAULT_LABEL_NAME = "mesos";
  private static final String DEFAULT_JVM_ARGS = "-Xms16m -XX:+UseConcMarkSweepGC -Djava.net.preferIPv4Stack=true";
  private static final String JVM_ARGS_PATTERN = "-Xmx.+ ";

  private final double slaveCpus;
  private final int slaveMem; // MB.
  private final double executorCpus;
  private final int maxExecutors;
  private final int executorMem; // MB.
  private final String remoteFSRoot;
  private final int idleTerminationMinutes;
  private final int maximumTimeToLiveMinutes;
  private final boolean useSlaveOnce;
  private final String jvmArgs;
  private final String jnlpArgs;
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated
  private transient JSONObject slaveAttributes;
  private String slaveAttributesString;
  private final ContainerInfo containerInfo;
  private final List<URI> additionalURIs;
  private final Mode mode;
  private final RunAsUserInfo runAsUserInfo;
  private final List<Command> additionalCommands;

  private final String labelString;

  private static final Logger LOGGER = Logger.getLogger(MesosSlaveInfo.class
      .getName());

  @DataBoundConstructor
  public MesosSlaveInfo(
          String labelString,
          Mode mode,
          String slaveCpus,
          String slaveMem,
          String maxExecutors,
          String executorCpus,
          String executorMem,
          String remoteFSRoot,
          String idleTerminationMinutes,
          String maximumTimeToLiveMinutes,
          boolean useSlaveOnce,
          String slaveAttributes,
          String jvmArgs,
          String jnlpArgs,
          ContainerInfo containerInfo,
          List<URI> additionalURIs,
          RunAsUserInfo runAsUserInfo,
          List<Command> additionalCommands)
          throws NumberFormatException {
    this(labelString,
         mode,
         slaveCpus,
         slaveMem,
         maxExecutors,
         executorCpus,
         executorMem,
         remoteFSRoot,
         idleTerminationMinutes,
         maximumTimeToLiveMinutes,
         useSlaveOnce,
         parseSlaveAttributes(slaveAttributes),
         jvmArgs,
         jnlpArgs,
         containerInfo,
         additionalURIs,
         runAsUserInfo,
         additionalCommands);
  }

  public MesosSlaveInfo(
      String labelString,
      Mode mode,
      String slaveCpus,
      String slaveMem,
      String maxExecutors,
      String executorCpus,
      String executorMem,
      String remoteFSRoot,
      String idleTerminationMinutes,
      String maximumTimeToLiveMinutes,
      boolean useSlaveOnce,
      JSONObject slaveAttributes,
      String jvmArgs,
      String jnlpArgs,
      ContainerInfo containerInfo,
      List<URI> additionalURIs,
      RunAsUserInfo runAsUserInfo,
      List<Command> additionalCommands)
      throws NumberFormatException {
    this.slaveCpus = Double.parseDouble(slaveCpus);
    this.slaveMem = Integer.parseInt(slaveMem);
    this.maxExecutors = Integer.parseInt(maxExecutors);
    this.executorCpus = Double.parseDouble(executorCpus);
    this.executorMem = Integer.parseInt(executorMem);
    this.remoteFSRoot = StringUtils.isNotBlank(remoteFSRoot) ? remoteFSRoot
        .trim() : "jenkins";
    this.idleTerminationMinutes = Integer.parseInt(idleTerminationMinutes);
    this.maximumTimeToLiveMinutes = Integer.parseInt(maximumTimeToLiveMinutes);
    this.useSlaveOnce = useSlaveOnce;
    this.labelString = StringUtils.isNotBlank(labelString) ? labelString
        : DEFAULT_LABEL_NAME;
    this.mode = mode != null ? mode : Mode.NORMAL;
    this.jvmArgs = StringUtils.isNotBlank(jvmArgs) ? cleanseJvmArgs(jvmArgs)
        : DEFAULT_JVM_ARGS;
    this.jnlpArgs = StringUtils.isNotBlank(jnlpArgs) ? jnlpArgs : "";
    this.containerInfo = containerInfo;
    this.additionalURIs = additionalURIs;
    this.additionalCommands = additionalCommands;
    this.slaveAttributesString = slaveAttributes != null ? slaveAttributes.toString() : null;
    this.runAsUserInfo = runAsUserInfo;
  }

  private static JSONObject parseSlaveAttributes(String slaveAttributes) {
    if (StringUtils.isNotBlank(slaveAttributes)) {
      try {
        return (JSONObject) JSONSerializer.toJSON(slaveAttributes);
      } catch (JSONException e) {
        LOGGER.warning("Ignoring Mesos slave attributes JSON due to parsing error : " + slaveAttributes);
      }
    }

    return null;
  }

  @Exported
  public String getLabelString() {
    return labelString;
  }

  public Mode getMode() {
    return mode;
  }

  public double getExecutorCpus() {
    return executorCpus;
  }

  public double getSlaveCpus() {
    return slaveCpus;
  }

  public int getSlaveMem() {
    return slaveMem;
  }

  public int getMaxExecutors() {
    return maxExecutors;
  }

  public int getExecutorMem() {
    return executorMem;
  }

  public String getRemoteFSRoot() {
    return remoteFSRoot;
  }

  public int getIdleTerminationMinutes() {
    return idleTerminationMinutes;
  }

  public int getMaximumTimeToLiveMinutes() {
    return maximumTimeToLiveMinutes;
  }

  public boolean isUseSlaveOnce() {
    return useSlaveOnce;
  }

  public JSONObject getSlaveAttributes() {
    return parseSlaveAttributes(slaveAttributesString);
  }

  public String getJvmArgs() {
    return jvmArgs;
  }

  public String getJnlpArgs() {
    return jnlpArgs;
  }

  @Exported(inline = true, visibility = 1)
  public ContainerInfo getContainerInfo() {
    return containerInfo;
  }

  public List<URI> getAdditionalURIs() {
    return additionalURIs;
  }

  public RunAsUserInfo getRunAsUserInfo() {
    return runAsUserInfo;
  }

  public List<Command> getAdditionalCommands() {
    return additionalCommands;
  }

  /**
   * Removes any additional {@code -Xmx} JVM args from the provided JVM
   * arguments. This is to ensure that the logic that sets the maximum heap
   * sized based on the memory available to the slave is not overriden by a
   * value provided via the configuration that may not work with the current
   * slave's configuration.
   *
   * @param jvmArgs
   *          the string of JVM arguments.
   * @return The cleansed JVM argument string.
   */
  private String cleanseJvmArgs(final String jvmArgs) {
    return jvmArgs.replaceAll(JVM_ARGS_PATTERN, "");
  }


  @SuppressWarnings("deprecation")
  public Object readResolve() {
    if (slaveAttributes != null) {
      slaveAttributesString = slaveAttributes.toString();
      slaveAttributes = null;
    }

    return this;
  }

  public static class RunAsUserInfo {
      public  final static String TOKEN_USERNAME = "{USERNAME}";
      public  final static String TOKEN_SLAVE_COMMAND = "{SLAVE_CMD}";

      private final String username;
      private final String command;

      @DataBoundConstructor
      public RunAsUserInfo(String username, String command) {
          this.username = username;
          this.command = command;
    }

    public String getUsername() {
        return username;
    }

      public String getCommand() {
          return command;
      }
  }

  @ExportedBean
  public static class ContainerInfo {
    private final String type;
    private final String dockerImage;
    private final List<Volume> volumes;
    private final List<Parameter> parameters;
    private final String networking;
    private static final String DEFAULT_NETWORKING = Network.BRIDGE.name();
    private final boolean useCustomDockerCommandShell;
    private final String customDockerCommandShell;
    private final Boolean dockerPrivilegedMode;
    private final Boolean dockerForcePullImage;

    private transient Map<Network, Set<PortMapping>> portMappingsMap;
    private Set<PortMapping> portMappings;

    @DataBoundConstructor
    public ContainerInfo(String type,
                         String dockerImage,
                         Boolean dockerPrivilegedMode,
                         Boolean dockerForcePullImage,
                         boolean useCustomDockerCommandShell,
                         String customDockerCommandShell,
                         List<Volume> volumes,
                         List<Parameter> parameters,
                         String networking,
                         Set<PortMapping> portMappings) {
      this.type = type;
      this.dockerImage = dockerImage;
      this.dockerPrivilegedMode = dockerPrivilegedMode;
      this.dockerForcePullImage = dockerForcePullImage;
      this.useCustomDockerCommandShell = useCustomDockerCommandShell;
      this.customDockerCommandShell = customDockerCommandShell;
      this.volumes = volumes;
      this.parameters = parameters;

      if (networking == null) {
        this.networking = DEFAULT_NETWORKING;
      } else {
        this.networking = networking;
      }

      if (Network.HOST.equals(Network.valueOf(networking))) {
        this.portMappings = Collections.emptySet();
      } else {
        this.portMappings = portMappings;
      }

      initPortMappings();
    }

    public ContainerInfo(String type,
                         String dockerImage,
                         Boolean dockerPrivilegedMode,
                         Boolean dockerForcePullImage,
                         boolean useCustomDockerCommandShell,
                         String customDockerCommandShell,
                         List<Volume> volumes,
                         List<Parameter> parameters,
                         String networking,
                         List<PortMapping> portMappings) {
      this(type, dockerImage, dockerPrivilegedMode, dockerForcePullImage,
              useCustomDockerCommandShell, customDockerCommandShell, volumes, parameters,
              networking, new LinkedHashSet<>(portMappings));
    }


    private void initPortMappings() {
      this.portMappingsMap = new HashMap<>();
      this.portMappingsMap.put(Network.valueOf(networking),this.portMappings);
    }

    @SuppressWarnings("unused")
    public Object readResolve() {
      if (portMappingsMap == null && portMappings != null) {
        initPortMappings();
      }

      return this;
    }

    public String getType() {
      return type;
    }

    @Exported
    public String getDockerImage() {
      return dockerImage;
    }

    public List<Volume> getVolumes() {
      return volumes;
    }

    public List<Parameter> getParameters() {
      return parameters;
    }

    public String getNetworking() {
      if (networking != null) {
        return networking;
      } else {
        return DEFAULT_NETWORKING;
      }
    }

    private Set<PortMapping> getPortMappings(Network currentNetworking) {
      if (portMappingsMap != null && portMappingsMap.get(currentNetworking) != null) {
        return Collections.unmodifiableSet(portMappingsMap.get(currentNetworking));
      } else {
        return Collections.emptySet();
      }
    }

    public Set<PortMapping> getPortMappings() {
      return getPortMappings(Network.valueOf(networking));
    }

    @SuppressWarnings("unused")
    public Set<PortMapping> getBridgePortMappings() {
      return getPortMappings(Network.BRIDGE);
    }

    @SuppressWarnings("unused")
    public Set<PortMapping> getUserPortMappings() {
      return getPortMappings(Network.USER);
    }

    public boolean hasPortMappings() {
      return portMappings != null && !portMappings.isEmpty();
    }

    public Boolean getDockerPrivilegedMode() {
      return dockerPrivilegedMode;
    }

    public Boolean getDockerForcePullImage() {
      return dockerForcePullImage;
    }

    public boolean getUseCustomDockerCommandShell() {  return useCustomDockerCommandShell; }

    public String getCustomDockerCommandShell() {  return customDockerCommandShell; }

  }

  public static class Parameter {
    private final String key;
    private final String value;

    @DataBoundConstructor
    public Parameter(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }
  }

  public static class PortMapping {

    private final Integer containerPort;
    private final Integer hostPort;
    private final String protocol;
    private final String description;
    private final String urlFormat;

    private transient final boolean staticHostPort;

    @DataBoundConstructor
    public PortMapping(Integer containerPort, Integer hostPort, String protocol, String description, String urlFormat) {
      this(containerPort, hostPort, protocol, description, urlFormat, hostPort != null);
    }

    public PortMapping(Integer containerPort, Integer hostPort, String protocol, String description, String urlFormat, boolean staticHostPort) {
      this.containerPort = containerPort;
      this.hostPort = hostPort;
      this.protocol = protocol;
      this.description = description;
      this.urlFormat = urlFormat;

      this.staticHostPort = staticHostPort;
    }

    public Integer getContainerPort() {
        return containerPort;
    }

    public Integer getHostPort() {
        return hostPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public String getUrlFormat() {
        return urlFormat;
    }

    @SuppressWarnings("unused")
    public boolean hasUrlFormat() {
        return urlFormat != null;
    }

    @SuppressWarnings("unused")
    public String getFormattedUrl(String hostname, Integer hostPort) {
        String[] searchList = {"{hostname}", "{hostPort}"};
        String[] replaceList = {hostname, String.valueOf(hostPort)};
        return StringUtils.replaceEach(urlFormat, searchList, replaceList);

    }

    public boolean isStaticHostPort() {
      return staticHostPort;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) { return false; }
      if (obj == this) { return true; }
      if (obj.getClass() != getClass()) {
        return false;
      }
      PortMapping rhs = (PortMapping) obj;
      return new EqualsBuilder()
              .append(containerPort, rhs.containerPort)
              .append(hostPort, rhs.hostPort)
              .append(protocol, rhs.protocol)
              .isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(19, 57)
              .append(containerPort)
              .append(hostPort)
              .append(protocol)
              .toHashCode();
    }

  }

  public static class Volume {
    private final String containerPath;
    private final String hostPath;
    private final boolean readOnly;

    @DataBoundConstructor
    public Volume(String containerPath, String hostPath, boolean readOnly) {
      this.containerPath = containerPath;
      this.hostPath = hostPath;
      this.readOnly = readOnly;
    }

    public String getContainerPath() {
      return containerPath;
    }

    public String getHostPath() {
      return hostPath;
    }

    public boolean isReadOnly() {
      return readOnly;
    }
  }

  public static class Command {
      private final String value;

      @DataBoundConstructor
      public Command(String value) {
        this.value = value;
      }

      public String getValue() {
        return value;
      }

    }

  public static class URI {
    private final String value;
    private final boolean executable;
    private final boolean extract;

    @DataBoundConstructor
    public URI(String value, boolean executable, boolean extract) {
      this.value = value;
      this.executable = executable;
      this.extract = extract;
    }

    public String getValue() {
      return value;
    }

    public boolean isExecutable() {
      return executable;
    }

    public boolean isExtract() {
      return extract;
    }
  }
}
