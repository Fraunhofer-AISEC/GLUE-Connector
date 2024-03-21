package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Container configuration that depends on the host we are running on
 *
 * @param cpuShares An integer value representing this container's relative CPU weight versus other containers.
 * @param memory Memory limit in bytes.
 * @param cgroupParent Path to `cgroups` under which the container's `cgroup` is created. If the path is not absolute, the path is considered to be relative to the `cgroups` path of the init process. Cgroups are created if they do not already exist.
 * @param blkioWeight Block IO weight (relative weight).
 * @param blkioWeightDevice Block IO weight (relative device weight) in the form:  ``` [{\"Path\": \"device_path\", \"Weight\": weight}] ```
 * @param blkioDeviceReadBps Limit read rate (bytes per second) from a device, in the form:  ``` [{\"Path\": \"device_path\", \"Rate\": rate}] ```
 * @param blkioDeviceWriteBps Limit write rate (bytes per second) to a device, in the form:  ``` [{\"Path\": \"device_path\", \"Rate\": rate}] ```
 * @param blkioDeviceReadIOps Limit read rate (IO per second) from a device, in the form:  ``` [{\"Path\": \"device_path\", \"Rate\": rate}] ```
 * @param blkioDeviceWriteIOps Limit write rate (IO per second) to a device, in the form:  ``` [{\"Path\": \"device_path\", \"Rate\": rate}] ```
 * @param cpuPeriod The length of a CPU period in microseconds.
 * @param cpuQuota Microseconds of CPU time that the container can get in a CPU period.
 * @param cpuRealtimePeriod The length of a CPU real-time period in microseconds. Set to 0 to allocate no time allocated to real-time tasks.
 * @param cpuRealtimeRuntime The length of a CPU real-time runtime in microseconds. Set to 0 to allocate no time allocated to real-time tasks.
 * @param cpusetCpus CPUs in which to allow execution (e.g., `0-3`, `0,1`).
 * @param cpusetMems Memory nodes (MEMs) in which to allow execution (0-3, 0,1). Only effective on NUMA systems.
 * @param devices A list of devices to add to the container.
 * @param deviceCgroupRules a list of cgroup rules to apply to the container
 * @param deviceRequests A list of requests for devices to be sent to device drivers.
 * @param kernelMemoryTCP Hard limit for kernel TCP buffer memory (in bytes). Depending on the OCI runtime in use, this option may be ignored. It is no longer supported by the default (runc) runtime.  This field is omitted when empty.
 * @param memoryReservation Memory soft limit in bytes.
 * @param memorySwap Total memory limit (memory + swap). Set as `-1` to enable unlimited swap.
 * @param memorySwappiness Tune a container's memory swappiness behavior. Accepts an integer between 0 and 100.
 * @param nanoCpus CPU quota in units of 10<sup>-9</sup> CPUs.
 * @param oomKillDisable Disable OOM Killer for the container.
 * @param `init` Run an init inside the container that forwards signals and reaps processes. This field is omitted if empty, and the default (as configured on the daemon) is used.
 * @param pidsLimit Tune a container's PIDs limit. Set `0` or `-1` for unlimited, or `null` to not change.
 * @param ulimits A list of resource limits to set in the container. For example:  ``` {\"Name\": \"nofile\", \"Soft\": 1024, \"Hard\": 2048} ```
 * @param cpuCount The number of usable CPUs (Windows only).  On Windows Server containers, the processor resource controls are mutually exclusive. The order of precedence is `CPUCount` first, then `CPUShares`, and `CPUPercent` last.
 * @param cpuPercent The usable percentage of the available CPUs (Windows only).  On Windows Server containers, the processor resource controls are mutually exclusive. The order of precedence is `CPUCount` first, then `CPUShares`, and `CPUPercent` last.
 * @param ioMaximumIOps Maximum IOps for the container system drive (Windows only)
 * @param ioMaximumBandwidth Maximum IO in bytes per second for the container system drive (Windows only).
 * @param binds A list of volume bindings for this container. Each volume binding is a string in one of these forms:  - `host-src:container-dest[:options]` to bind-mount a host path   into the container. Both `host-src`, and `container-dest` must   be an _absolute_ path. - `volume-name:container-dest[:options]` to bind-mount a volume   managed by a volume driver into the container. `container-dest`   must be an _absolute_ path.  `options` is an optional, comma-delimited list of:  - `nocopy` disables automatic copying of data from the container   path to the volume. The `nocopy` flag only applies to named volumes. - `[ro|rw]` mounts a volume read-only or read-write, respectively.   If omitted or set to `rw`, volumes are mounted read-write. - `[z|Z]` applies SELinux labels to allow or deny multiple containers   to read and write to the same volume.     - `z`: a _shared_ content label is applied to the content. This       label indicates that multiple containers can share the volume       content, for both reading and writing.     - `Z`: a _private unshared_ label is applied to the content.       This label indicates that only the current container can use       a private volume. Labeling systems such as SELinux require       proper labels to be placed on volume content that is mounted       into a container. Without a label, the security system can       prevent a container's processes from using the content. By       default, the labels set by the host operating system are not       modified. - `[[r]shared|[r]slave|[r]private]` specifies mount   [propagation behavior](https://www.kernel.org/doc/Documentation/filesystems/sharedsubtree.txt).   This only applies to bind-mounted volumes, not internal volumes   or named volumes. Mount propagation requires the source mount   point (the location where the source directory is mounted in the   host operating system) to have the correct propagation properties.   For shared volumes, the source mount point must be set to `shared`.   For slave volumes, the mount must be set to either `shared` or   `slave`.
 * @param containerIDFile Path to a file where the container ID is written
 * @param logConfig
 * @param networkMode Network mode to use for this container. Supported standard values are: `bridge`, `host`, `none`, and `container:<name|id>`. Any other value is taken as a custom network's name to which this container should connect to.
 * @param portBindings PortMap describes the mapping of container ports to host ports, using the container's port-number and protocol as key in the format `<port>/<protocol>`, for example, `80/udp`.  If a container's port is mapped for multiple protocols, separate entries are added to the mapping table.
 * @param restartPolicy
 * @param autoRemove Automatically remove the container when the container's process exits. This has no effect if `RestartPolicy` is set.
 * @param volumeDriver Driver that this container uses to mount volumes.
 * @param volumesFrom A list of volumes to inherit from another container, specified in the form `<container name>[:<ro|rw>]`.
 * @param mounts Specification for mounts to be added to the container.
 * @param consoleSize Initial console size, as an `[height, width]` array.
 * @param annotations Arbitrary non-identifying metadata attached to container and provided to the runtime when the container is started.
 * @param capAdd A list of kernel capabilities to add to the container. Conflicts with option 'Capabilities'.
 * @param capDrop A list of kernel capabilities to drop from the container. Conflicts with option 'Capabilities'.
 * @param cgroupnsMode cgroup namespace mode for the container. Possible values are:  - `\"private\"`: the container runs in its own private cgroup namespace - `\"host\"`: use the host system's cgroup namespace  If not specified, the daemon default is used, which can either be `\"private\"` or `\"host\"`, depending on daemon version, kernel support and configuration.
 * @param dns A list of DNS servers for the container to use.
 * @param dnsOptions A list of DNS options.
 * @param dnsSearch A list of DNS search domains.
 * @param extraHosts A list of hostnames/IP mappings to add to the container's `/etc/hosts` file. Specified in the form `[\"hostname:IP\"]`.
 * @param groupAdd A list of additional groups that the container process will run as.
 * @param ipcMode IPC sharing mode for the container. Possible values are:  - `\"none\"`: own private IPC namespace, with /dev/shm not mounted - `\"private\"`: own private IPC namespace - `\"shareable\"`: own private IPC namespace, with a possibility to share it with other containers - `\"container:<name|id>\"`: join another (shareable) container's IPC namespace - `\"host\"`: use the host system's IPC namespace  If not specified, daemon default is used, which can either be `\"private\"` or `\"shareable\"`, depending on daemon version and configuration.
 * @param cgroup Cgroup to use for the container.
 * @param links A list of links for the container in the form `container_name:alias`.
 * @param oomScoreAdj An integer value containing the score given to the container in order to tune OOM killer preferences.
 * @param pidMode Set the PID (Process) Namespace mode for the container. It can be either:  - `\"container:<name|id>\"`: joins another container's PID namespace - `\"host\"`: use the host's PID namespace inside the container
 * @param privileged Gives the container full access to the host.
 * @param publishAllPorts Allocates an ephemeral host port for all of a container's exposed ports.  Ports are de-allocated when the container stops and allocated when the container starts. The allocated port might be changed when restarting the container.  The port is selected from the ephemeral port range that depends on the kernel. For example, on Linux the range is defined by `/proc/sys/net/ipv4/ip_local_port_range`.
 * @param readonlyRootfs Mount the container's root filesystem as read only.
 * @param securityOpt A list of string values to customize labels for MLS systems, such as SELinux.
 * @param storageOpt Storage driver options for this container, in the form `{\"size\": \"120G\"}`.
 * @param tmpfs A map of container directories which should be replaced by tmpfs mounts, and their corresponding mount options. For example:  ``` { \"/run\": \"rw,noexec,nosuid,size=65536k\" } ```
 * @param utSMode UTS namespace to use for the container.
 * @param usernsMode Sets the usernamespace mode for the container when usernamespace remapping option is enabled.
 * @param shmSize Size of `/dev/shm` in bytes. If omitted, the system uses 64MB.
 * @param sysctls A list of kernel parameters (sysctls) to set in the container. For example:  ``` {\"net.ipv4.ip_forward\": \"1\"} ```
 * @param runtime Runtime to use with this container.
 * @param isolation Isolation technology of the container. (Windows only)
 * @param maskedPaths The list of paths to be masked inside the container (this overrides the default set of paths).
 * @param readonlyPaths The list of paths to be set as read-only inside the container (this overrides the default set of paths).
 */
@Serializable
data class HostConfig(
    // An integer value representing this container's relative CPU weight versus other containers.
    @SerialName(value = "CpuShares")
    val cpuShares: Int? = null,
    // Memory limit in bytes.
    @SerialName(value = "Memory")
    val memory: Long? = null,
    // Path to `cgroups` under which the container's `cgroup` is created. If the path is not absolute, the path is considered to be relative to the `cgroups` path of the init process. Cgroups are created if they do not already exist.
    @SerialName(value = "CgroupParent")
    val cgroupParent: String? = null,
    // Block IO weight (relative weight).
    @SerialName(value = "BlkioWeight")
    val blkioWeight: Int? = null,
    // Block IO weight (relative device weight) in the form:  ``` [{\"Path\": \"device_path\", \"Weight\": weight}] ```
    @SerialName(value = "BlkioWeightDevice")
    val blkioWeightDevice: List<BlkioWeightDevice>? = null,
    // Limit read rate (bytes per second) from a device, in the form:  ``` [{\"Path\": \"device_path\", \"Rate\": rate}] ```
    @SerialName(value = "BlkioDeviceReadBps")
    val blkioDeviceReadBps: List<ThrottleDevice>? = null,
    // Limit write rate (bytes per second) to a device, in the form:  ``` [{\"Path\": \"device_path\", \"Rate\": rate}] ```
    @SerialName(value = "BlkioDeviceWriteBps")
    val blkioDeviceWriteBps: List<ThrottleDevice>? = null,
    // Limit read rate (IO per second) from a device, in the form:  ``` [{\"Path\": \"device_path\", \"Rate\": rate}] ```
    @SerialName(value = "BlkioDeviceReadIOps")
    val blkioDeviceReadIOps: List<ThrottleDevice>? = null,
    // Limit write rate (IO per second) to a device, in the form:  ``` [{\"Path\": \"device_path\", \"Rate\": rate}] ```
    @SerialName(value = "BlkioDeviceWriteIOps")
    val blkioDeviceWriteIOps: List<ThrottleDevice>? = null,
    // The length of a CPU period in microseconds.
    @SerialName(value = "CpuPeriod")
    val cpuPeriod: Long? = null,
    // Microseconds of CPU time that the container can get in a CPU period.
    @SerialName(value = "CpuQuota")
    val cpuQuota: Long? = null,
    // The length of a CPU real-time period in microseconds. Set to 0 to allocate no time allocated to real-time tasks.
    @SerialName(value = "CpuRealtimePeriod")
    val cpuRealtimePeriod: Long? = null,
    // The length of a CPU real-time runtime in microseconds. Set to 0 to allocate no time allocated to real-time tasks.
    @SerialName(value = "CpuRealtimeRuntime")
    val cpuRealtimeRuntime: Long? = null,
    // CPUs in which to allow execution (e.g., `0-3`, `0,1`).
    @SerialName(value = "CpusetCpus")
    val cpusetCpus: String? = null,
    // Memory nodes (MEMs) in which to allow execution (0-3, 0,1). Only effective on NUMA systems.
    @SerialName(value = "CpusetMems")
    val cpusetMems: String? = null,
    // A list of devices to add to the container.
    @SerialName(value = "Devices")
    val devices: List<DeviceMapping>? = null,
    // a list of cgroup rules to apply to the container
    @SerialName(value = "DeviceCgroupRules")
    val deviceCgroupRules: List<String>? = null,
    // A list of requests for devices to be sent to device drivers.
    @SerialName(value = "DeviceRequests")
    val deviceRequests: List<DeviceRequest>? = null,
    // Hard limit for kernel TCP buffer memory (in bytes). Depending on the OCI runtime in use, this option may be ignored. It is no longer supported by the default (runc) runtime.  This field is omitted when empty.
    @SerialName(value = "KernelMemoryTCP")
    val kernelMemoryTCP: Long? = null,
    // Memory soft limit in bytes.
    @SerialName(value = "MemoryReservation")
    val memoryReservation: Long? = null,
    // Total memory limit (memory + swap). Set as `-1` to enable unlimited swap.
    @SerialName(value = "MemorySwap")
    val memorySwap: Long? = null,
    // Tune a container's memory swappiness behavior. Accepts an integer between 0 and 100.
    @SerialName(value = "MemorySwappiness")
    val memorySwappiness: Long? = null,
    // CPU quota in units of 10<sup>-9</sup> CPUs.
    @SerialName(value = "NanoCpus")
    val nanoCpus: Long? = null,
    // Disable OOM Killer for the container.
    @SerialName(value = "OomKillDisable")
    val oomKillDisable: Boolean? = null,
    // Run an init inside the container that forwards signals and reaps processes. This field is omitted if empty, and the default (as configured on the daemon) is used.
    @SerialName(value = "Init")
    val `init`: Boolean? = null,
    // Tune a container's PIDs limit. Set `0` or `-1` for unlimited, or `null` to not change.
    @SerialName(value = "PidsLimit")
    val pidsLimit: Long? = null,
    // A list of resource limits to set in the container. For example:  ``` {\"Name\": \"nofile\", \"Soft\": 1024, \"Hard\": 2048} ```
    @SerialName(value = "Ulimits")
    val ulimits: List<Ulimit>? = null,
    // The number of usable CPUs (Windows only).  On Windows Server containers, the processor resource controls are mutually exclusive. The order of precedence is `CPUCount` first, then `CPUShares`, and `CPUPercent` last.
    @SerialName(value = "CpuCount")
    val cpuCount: Long? = null,
    // The usable percentage of the available CPUs (Windows only).  On Windows Server containers, the processor resource controls are mutually exclusive. The order of precedence is `CPUCount` first, then `CPUShares`, and `CPUPercent` last.
    @SerialName(value = "CpuPercent")
    val cpuPercent: Long? = null,
    // Maximum IOps for the container system drive (Windows only)
    @SerialName(value = "IOMaximumIOps")
    val ioMaximumIOps: Long? = null,
    // Maximum IO in bytes per second for the container system drive (Windows only).
    @SerialName(value = "IOMaximumBandwidth")
    val ioMaximumBandwidth: Long? = null,
    // A list of volume bindings for this container. Each volume binding is a string in one of these forms:  - `host-src:container-dest[:options]` to bind-mount a host path   into the container. Both `host-src`, and `container-dest` must   be an _absolute_ path. - `volume-name:container-dest[:options]` to bind-mount a volume   managed by a volume driver into the container. `container-dest`   must be an _absolute_ path.  `options` is an optional, comma-delimited list of:  - `nocopy` disables automatic copying of data from the container   path to the volume. The `nocopy` flag only applies to named volumes. - `[ro|rw]` mounts a volume read-only or read-write, respectively.   If omitted or set to `rw`, volumes are mounted read-write. - `[z|Z]` applies SELinux labels to allow or deny multiple containers   to read and write to the same volume.     - `z`: a _shared_ content label is applied to the content. This       label indicates that multiple containers can share the volume       content, for both reading and writing.     - `Z`: a _private unshared_ label is applied to the content.       This label indicates that only the current container can use       a private volume. Labeling systems such as SELinux require       proper labels to be placed on volume content that is mounted       into a container. Without a label, the security system can       prevent a container's processes from using the content. By       default, the labels set by the host operating system are not       modified. - `[[r]shared|[r]slave|[r]private]` specifies mount   [propagation behavior](https://www.kernel.org/doc/Documentation/filesystems/sharedsubtree.txt).   This only applies to bind-mounted volumes, not internal volumes   or named volumes. Mount propagation requires the source mount   point (the location where the source directory is mounted in the   host operating system) to have the correct propagation properties.   For shared volumes, the source mount point must be set to `shared`.   For slave volumes, the mount must be set to either `shared` or   `slave`.
    @SerialName(value = "Binds")
    val binds: List<String>? = null,
    // Path to a file where the container ID is written
    @SerialName(value = "ContainerIDFile")
    val containerIDFile: String? = null,
    @SerialName(value = "LogConfig")
    val logConfig: LogConfig? = null,
    // Network mode to use for this container. Supported standard values are: `bridge`, `host`, `none`, and `container:<name|id>`. Any other value is taken as a custom network's name to which this container should connect to.
    @SerialName(value = "NetworkMode")
    val networkMode: String? = null,
    // PortMap describes the mapping of container ports to host ports, using the container's port-number and protocol as key in the format `<port>/<protocol>`, for example, `80/udp`.  If a container's port is mapped for multiple protocols, separate entries are added to the mapping table.
    @SerialName(value = "PortBindings")
    val portBindings: Map<String, List<PortBinding>>? = null,
    @SerialName(value = "RestartPolicy")
    val restartPolicy: RestartPolicy? = null,
    // Automatically remove the container when the container's process exits. This has no effect if `RestartPolicy` is set.
    @SerialName(value = "AutoRemove")
    val autoRemove: Boolean? = null,
    // Driver that this container uses to mount volumes.
    @SerialName(value = "VolumeDriver")
    val volumeDriver: String? = null,
    // A list of volumes to inherit from another container, specified in the form `<container name>[:<ro|rw>]`.
    @SerialName(value = "VolumesFrom")
    val volumesFrom: List<String>? = null,
    // Specification for mounts to be added to the container.
    @SerialName(value = "Mounts")
    val mounts: List<Mount>? = null,
    // Initial console size, as an `[height, width]` array.
    @SerialName(value = "ConsoleSize")
    val consoleSize: List<Int>? = null,
    // Arbitrary non-identifying metadata attached to container and provided to the runtime when the container is started.
    @SerialName(value = "Annotations")
    val annotations: Map<String, String>? = null,
    // A list of kernel capabilities to add to the container. Conflicts with option 'Capabilities'.
    @SerialName(value = "CapAdd")
    val capAdd: List<String>? = null,
    // A list of kernel capabilities to drop from the container. Conflicts with option 'Capabilities'.
    @SerialName(value = "CapDrop")
    val capDrop: List<String>? = null,
    // cgroup namespace mode for the container. Possible values are:  - `\"private\"`: the container runs in its own private cgroup namespace - `\"host\"`: use the host system's cgroup namespace  If not specified, the daemon default is used, which can either be `\"private\"` or `\"host\"`, depending on daemon version, kernel support and configuration.
    @SerialName(value = "CgroupnsMode")
    val cgroupnsMode: String? = null,
    // A list of DNS servers for the container to use.
    @SerialName(value = "Dns")
    val dns: List<String>? = null,
    // A list of DNS options.
    @SerialName(value = "DnsOptions")
    val dnsOptions: List<String>? = null,
    // A list of DNS search domains.
    @SerialName(value = "DnsSearch")
    val dnsSearch: List<String>? = null,
    // A list of hostnames/IP mappings to add to the container's `/etc/hosts` file. Specified in the form `[\"hostname:IP\"]`.
    @SerialName(value = "ExtraHosts")
    val extraHosts: List<String>? = null,
    // A list of additional groups that the container process will run as.
    @SerialName(value = "GroupAdd")
    val groupAdd: List<String>? = null,
    // IPC sharing mode for the container. Possible values are:  - `\"none\"`: own private IPC namespace, with /dev/shm not mounted - `\"private\"`: own private IPC namespace - `\"shareable\"`: own private IPC namespace, with a possibility to share it with other containers - `\"container:<name|id>\"`: join another (shareable) container's IPC namespace - `\"host\"`: use the host system's IPC namespace  If not specified, daemon default is used, which can either be `\"private\"` or `\"shareable\"`, depending on daemon version and configuration.
    @SerialName(value = "IpcMode")
    val ipcMode: String? = null,
    // Cgroup to use for the container.
    @SerialName(value = "Cgroup")
    val cgroup: String? = null,
    // A list of links for the container in the form `container_name:alias`.
    @SerialName(value = "Links")
    val links: List<String>? = null,
    // An integer value containing the score given to the container in order to tune OOM killer preferences.
    @SerialName(value = "OomScoreAdj")
    val oomScoreAdj: Int? = null,
    // Set the PID (Process) Namespace mode for the container. It can be either:  - `\"container:<name|id>\"`: joins another container's PID namespace - `\"host\"`: use the host's PID namespace inside the container
    @SerialName(value = "PidMode")
    val pidMode: String? = null,
    // Gives the container full access to the host.
    @SerialName(value = "Privileged")
    val privileged: Boolean? = null,
    // Allocates an ephemeral host port for all of a container's exposed ports.  Ports are de-allocated when the container stops and allocated when the container starts. The allocated port might be changed when restarting the container.  The port is selected from the ephemeral port range that depends on the kernel. For example, on Linux the range is defined by `/proc/sys/net/ipv4/ip_local_port_range`.
    @SerialName(value = "PublishAllPorts")
    val publishAllPorts: Boolean? = null,
    // Mount the container's root filesystem as read only.
    @SerialName(value = "ReadonlyRootfs")
    val readonlyRootfs: Boolean? = null,
    // A list of string values to customize labels for MLS systems, such as SELinux.
    @SerialName(value = "SecurityOpt")
    val securityOpt: List<String>? = null,
    // Storage driver options for this container, in the form `{\"size\": \"120G\"}`.
    @SerialName(value = "StorageOpt")
    val storageOpt: Map<String, String>? = null,
    // A map of container directories which should be replaced by tmpfs mounts, and their corresponding mount options. For example:  ``` { \"/run\": \"rw,noexec,nosuid,size=65536k\" } ```
    @SerialName(value = "Tmpfs")
    val tmpfs: Map<String, String>? = null,
    // UTS namespace to use for the container.
    @SerialName(value = "UTSMode")
    val utSMode: String? = null,
    // Sets the usernamespace mode for the container when usernamespace remapping option is enabled.
    @SerialName(value = "UsernsMode")
    val usernsMode: String? = null,
    // Size of `/dev/shm` in bytes. If omitted, the system uses 64MB.
    @SerialName(value = "ShmSize")
    val shmSize: Long? = null,
    // A list of kernel parameters (sysctls) to set in the container. For example:  ``` {\"net.ipv4.ip_forward\": \"1\"} ```
    @SerialName(value = "Sysctls")
    val sysctls: Map<String, String>? = null,
    // Runtime to use with this container.
    @SerialName(value = "Runtime")
    val runtime: String? = null,
    // Isolation technology of the container. (Windows only)
    @SerialName(value = "Isolation")
    val isolation: String? = null,
    // The list of paths to be masked inside the container (this overrides the default set of paths).
    @SerialName(value = "MaskedPaths")
    val maskedPaths: List<String>? = null,
    // The list of paths to be set as read-only inside the container (this overrides the default set of paths).
    @SerialName(value = "ReadonlyPaths")
    val readonlyPaths: List<String>? = null,
)
