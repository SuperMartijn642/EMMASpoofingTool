package emma;

import emma.arp.ARPSpoofingAttackHandler;
import emma.dns.DNSSpoofingAttackHandler;

import java.util.function.Supplier;

/**
 * Created 12/06/2022 by SuperMartijn642
 */
public enum AttackMode {

    ARP_SPOOFING("ARP Spoofing", "Allows you to spoof an ip address by sending ARP packets.", ARPSpoofingAttackHandler::new),
    DNS_SPOOFING("DNS Spoofing", "Allows you to spoof DNS requests and map domain names to certain ip addresses.", DNSSpoofingAttackHandler::new);

    private final String title;
    private final String description;
    private final Supplier<AttackHandler> instanceCreator;

    AttackMode(String title, String description, Supplier<AttackHandler> instanceCreator){
        this.title = title;
        this.description = description;
        this.instanceCreator = instanceCreator;
    }

    public String getTitle(){
        return this.title;
    }

    public String getDescription(){
        return this.description;
    }

    public AttackHandler createInstance(){
        return this.instanceCreator.get();
    }

    @Override
    public String toString(){
        return this.getTitle();
    }
}
