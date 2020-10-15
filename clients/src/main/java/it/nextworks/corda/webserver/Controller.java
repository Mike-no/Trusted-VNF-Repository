package it.nextworks.corda.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@RestController
@RequestMapping("/") /* The paths for HTTP requests are relative to this base path. */
public class Controller {

    private final CordaRPCOps proxy;
    private final CordaX500Name me;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        proxy = rpc.getProxy();
        me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /* Helpers for filtering the network map cache */

    public String toDisplayString(X500Name name) { return BCStyle.INSTANCE.toString(name); }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities().stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo) { return nodeInfo.getLegalIdentities().get(0).getName().equals(me); }

    private boolean isNetworkMap(NodeInfo nodeInfo) {
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() { return JacksonSupport.createNonRpcMapper(); }
    }

    @GetMapping(value = "/status", produces = TEXT_PLAIN_VALUE)
    private String status() { return "200"; }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() { return LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC")).toString(); }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() { return proxy.nodeInfo().getAddresses().toString(); }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    private String identities() { return proxy.nodeInfo().getLegalIdentities().toString(); }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() { return Integer.toString(proxy.nodeInfo().getPlatformVersion()); }

    @GetMapping(value = "/notaries", produces = TEXT_PLAIN_VALUE)
    private String notaries() { return proxy.notaryIdentities().toString(); }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    private String flows() { return proxy.registeredFlows().toString(); }

    @GetMapping(value = "/me", produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami() {
        HashMap<String, String> map = new HashMap<>();
        map.put("me", me.toString());

        return map;
    }

    @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> map = new HashMap<>();

        /* Find all nodes that are not notaries, ourself, or the Network Map */
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream().filter(el ->
                !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        /* Get names ad strings */
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        map.put("peers", nodeNames);
        return map;
    }


}