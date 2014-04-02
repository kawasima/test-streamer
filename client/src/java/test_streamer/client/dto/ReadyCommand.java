package test_streamer.client.dto;

import us.bpsm.edn.Keyword;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A command for :ready
 *
 * @author kawasima
 */
public class ReadyCommand {
    private Keyword command = Keyword.newKeyword("ready");
    private String osName;
    private String osVersion;
    private String cpuArch;
    private int cpuCore;
    private String clientName;

    public ReadyCommand() {
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
        osName = mxBean.getName();
        cpuArch = mxBean.getArch();
        osVersion = mxBean.getVersion();
        cpuCore = mxBean.getAvailableProcessors();
        try {
            clientName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            clientName = "(none)";
        }
    }

    public Keyword getCommand() {
        return command;
    }

    public String getClientName() {
        return clientName;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getCpuArch() {
        return cpuArch;
    }

    public int getCpuCore() {
        return cpuCore;
    }
}
