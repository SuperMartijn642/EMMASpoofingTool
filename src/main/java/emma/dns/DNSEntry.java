package emma.dns;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * Created 14/06/2022 by SuperMartijn642
 */
public class DNSEntry {

    public final boolean regex;
    public final String domain;
    public final InetAddress ip;
    private final boolean isV4;

    public DNSEntry(boolean regex, String domain, InetAddress ip){
        this.regex = regex;
        this.domain = domain;
        this.ip = ip;
        this.isV4 = ip instanceof Inet4Address;
    }

    public boolean matches(String domain){
        return this.regex ? domain.matches(this.domain) : this.domain.equals(domain);
    }

    public InetAddress getIp(){
        return this.ip;
    }

    public boolean isIpV4(){
        return this.isV4;
    }

    @Override
    public String toString(){
        return (this.regex ? "/" + this.domain + "/" : "'" + this.domain + "'")
            + (this.isV4 ? " --> A --> " : " --> AAAA --> ")
            + "'" + this.ip.getHostAddress() + "'";
    }
}
