package emma;

import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.util.ByteArrays;
import org.pcap4j.util.MacAddress;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Random;

/**
 * Created 12/06/2022 by SuperMartijn642
 */
public class PacketUtils {

    /**
     * Creates an arp 'who-has' request packet to ask who knows the mac-address corresponding to dst_ip and send to it back to src_ip/src_mac.
     * @param srcIp  ip address of the sender
     * @param srcMac mac address of the sender
     * @param dstIp  ip address of the target
     * @return an arp request packet with the given properties
     */
    public static ArpPacket.Builder createARPRequestPacket(InetAddress srcIp, MacAddress srcMac, InetAddress dstIp){
        return new ArpPacket.Builder()
            .operation(ArpOperation.REQUEST)
            .hardwareType(ArpHardwareType.ETHERNET)
            .protocolType(EtherType.IPV4)
            .hardwareAddrLength((byte)MacAddress.SIZE_IN_BYTES)
            .protocolAddrLength((byte)ByteArrays.INET4_ADDRESS_SIZE_IN_BYTES)
            .srcHardwareAddr(srcMac)
            .srcProtocolAddr(srcIp)
            .dstHardwareAddr(MacAddress.getByName("00:00:00:00:00:00"))
            .dstProtocolAddr(dstIp);
    }

    /**
     * Creates an arp 'is-at' response packet for dstIp/dstMac to map srcIp to srcMac.
     * @param srcIp  ip address of the sender
     * @param srcMac mac address of the sender
     * @param dstIp  ip address of the destination
     * @param dstMac mac address of the destination
     * @return an arp request packet with the given properties
     */
    public static ArpPacket.Builder createARPResponsePacket(InetAddress srcIp, MacAddress srcMac, InetAddress dstIp, MacAddress dstMac){
        return new ArpPacket.Builder()
            .operation(ArpOperation.REPLY)
            .hardwareType(ArpHardwareType.ETHERNET)
            .protocolType(EtherType.IPV4)
            .hardwareAddrLength((byte)MacAddress.SIZE_IN_BYTES)
            .protocolAddrLength((byte)ByteArrays.INET4_ADDRESS_SIZE_IN_BYTES)
            .srcHardwareAddr(srcMac)
            .srcProtocolAddr(srcIp)
            .dstHardwareAddr(dstMac)
            .dstProtocolAddr(dstIp);
    }

    /**
     * Wraps the given payload in an ethernet packet.
     * @param srcMac      ip address of the sender
     * @param payloadType type of the payload
     * @param payload     the contents of the ethernet packet
     * @return an ethernet packet with the given payload as content
     * @see EtherType
     */
    public static EthernetPacket.Builder wrapInEthernetPacket(MacAddress srcMac, MacAddress dstMac, EtherType payloadType, Packet.Builder payload){
        return new EthernetPacket.Builder()
            .dstAddr(dstMac)
            .srcAddr(srcMac)
            .type(payloadType)
            .payloadBuilder(payload)
            .paddingAtBuild(true);
    }

    /**
     * Wraps the given payload in a UDP packet.
     * @param srcPort originating port number
     * @param dstPort destination port number
     * @param payload the contents of the UDP packet
     * @return a UDP packet with the given payload as content
     */
    public static UdpPacket.Builder wrapInUDPPacket(InetAddress srcAddress, UdpPort srcPort, InetAddress dstAddress, UdpPort dstPort, Packet.Builder payload){
        return new UdpPacket.Builder()
            .srcPort(srcPort)
            .dstPort(dstPort)
            .srcAddr(srcAddress)
            .dstAddr(dstAddress)
            .payloadBuilder(payload)
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true);
    }

    /**
     * Wraps the given payload in an IPv4 packet.
     * @param srcIp   originating address
     * @param dstIp   destination address
     * @param payload the contents of the IPv4 packet
     * @return an IPv4 packet with the given payload as content
     * @see IpNumber
     */
    public static IpV4Packet.Builder wrapInIPv4Packet(Inet4Address srcIp, Inet4Address dstIp, IpNumber payloadHeader, Packet.Builder payload){
        return new IpV4Packet.Builder()
            .version(IpVersion.IPV4)
            .tos(IpV4Rfc1349Tos.newInstance((byte)0))
            .identification((short)new Random().nextInt())
            .reservedFlag(false)
            .dontFragmentFlag(false)
            .moreFragmentFlag(false)
            .fragmentOffset((short)0)
            .ttl((byte)60)
            .protocol(payloadHeader)
            .srcAddr(srcIp)
            .dstAddr(dstIp)
            .payloadBuilder(payload)
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true);
    }

    /**
     * Wraps the given payload in an IPv6 packet.
     * @param srcIp   originating address
     * @param dstIp   destination address
     * @param payload the contents of the IPv6 packet
     * @return an IPv6 packet with the given payload as content
     * @see IpNumber
     */
    public static IpV6Packet.Builder wrapInIPv6Packet(Inet6Address srcIp, Inet6Address dstIp, byte hopLimit, IpNumber payloadHeader, Packet.Builder payload){
        return new IpV6Packet.Builder()
            .version(IpVersion.IPV6)
            .trafficClass(IpV6SimpleTrafficClass.newInstance((byte)0))
            .flowLabel(IpV6SimpleFlowLabel.newInstance(0))
            .nextHeader(payloadHeader)
            .hopLimit(hopLimit)
            .srcAddr(srcIp)
            .dstAddr(dstIp)
            .payloadBuilder(payload)
            .correctLengthAtBuild(true);
    }
}
