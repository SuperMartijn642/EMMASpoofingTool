package emma;

import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import java.util.Collections;
import java.util.List;

/**
 * Created 12/06/2022 by SuperMartijn642
 */
public class NetworkUtils {

    public static final int SNAPSHOT_LENGTH = 65536;
    public static final int CONNECTION_TIMEOUT = 1000;

    /**
     * Gathers a list of all available network interfaces on the system.
     * @return a list of network interfaces
     * @see Pcaps#findAllDevs()
     */
    public static List<PcapNetworkInterface> gatherNetworkInterfaces(){
        try{
            return Collections.unmodifiableList(Pcaps.findAllDevs());
        }catch(PcapNativeException e){
            Main.displayWarning("Failed to gather available network interfaces!", e);
            return Collections.emptyList();
        }
    }

    /**
     * Opens a pcap connection on the given network interface.
     * @param networkInterface the interface to open a connection on
     * @return an open connection on the network interface
     * @throws PcapNativeException when a system error occurs during connection setup
     * @see PcapNetworkInterface#openLive(int, PcapNetworkInterface.PromiscuousMode, int)
     */
    public static PcapHandle openConnection(PcapNetworkInterface networkInterface, int timeout) throws PcapNativeException{
        return networkInterface.openLive(SNAPSHOT_LENGTH, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, timeout);
    }
}
