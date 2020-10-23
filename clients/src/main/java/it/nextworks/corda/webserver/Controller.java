package it.nextworks.corda.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextworks.corda.flows.EstablishFeeAgreementFlow;
import it.nextworks.corda.flows.GetPkgsFlow;
import it.nextworks.corda.flows.RegisterPkgFlow;
import it.nextworks.corda.flows.SelfIssueCashFlow;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.contracts.asset.Cash;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.corda.finance.workflows.GetBalances.getCashBalances;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@RestController
@RequestMapping("/") /* The paths for HTTP requests are relative to this base path. */
public class Controller {

    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    @Value("${config.catalogue.host}")
    private String catalogueURL;

    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        proxy = rpc.getProxy();
        me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    private class RegisterPkgWrapper {
        private final String name;
        private final String description;
        private final String version;
        private final String pkgInfoId;
        private final String imageLink;
        private final PkgOfferState.PkgType pkgType;
        private final ProductOfferingPrice poPrice;

        public RegisterPkgWrapper(String name,
                                  String description,
                                  String version,
                                  String pkgInfoId,
                                  String imageLink,
                                  PkgOfferState.PkgType pkgType,
                                  ProductOfferingPrice poPrice) {
            this.name = name;
            this.description = description;
            this.version = version;
            this.pkgInfoId = pkgInfoId;
            this.imageLink = imageLink;
            this.pkgType = pkgType;
            this.poPrice = poPrice;
        }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public String getVersion() { return version; }

        public String getPkgInfoId() { return pkgInfoId; }

        public String getImageLink() { return imageLink; }

        public PkgOfferState.PkgType getPkgType() { return pkgType; }

        public ProductOfferingPrice getPoPrice() { return poPrice; }
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

    @GetMapping(value = "status", produces = TEXT_PLAIN_VALUE)
    private String status() { return "200"; }

    @GetMapping(value = "servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() { return LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC")).toString(); }

    @GetMapping(value = "addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() { return proxy.nodeInfo().getAddresses().toString(); }

    @GetMapping(value = "identities", produces = TEXT_PLAIN_VALUE)
    private String identities() { return proxy.nodeInfo().getLegalIdentities().toString(); }

    @GetMapping(value = "platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() { return Integer.toString(proxy.nodeInfo().getPlatformVersion()); }

    @GetMapping(value = "notaries", produces = TEXT_PLAIN_VALUE)
    private String notaries() { return proxy.notaryIdentities().toString(); }

    @GetMapping(value = "flows", produces = TEXT_PLAIN_VALUE)
    private String flows() { return proxy.registeredFlows().toString(); }

    @GetMapping(value = "me", produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami() {
        HashMap<String, String> map = new HashMap<>();
        map.put("me", me.toString());

        return map;
    }

    @GetMapping(value = "cash-balances", produces = APPLICATION_JSON_VALUE)
    public Map<Currency, Amount<Currency>> cashBalances(){
        return getCashBalances(proxy);
    }

    @GetMapping(value = "peers", produces = APPLICATION_JSON_VALUE)
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

    @PutMapping(value = "dev/establish-fee-agreement", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> establishFeeAgreement(@RequestParam(value = "maxAcceptableFee") int maxAcceptableFee) {
        try {
            proxy.startTrackedFlowDynamic(EstablishFeeAgreementFlow.DevInitiation.class, maxAcceptableFee)
                    .getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("Fee Agreement established with a fee of 10%.");
        } catch(IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(iae.getMessage());
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "register-pkg", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> registerPkg(@RequestBody RegisterPkgWrapper wrapper) {
        try {
            String pkgInfoId = wrapper.getPkgInfoId();

            /* Make GET request to 5g-catalogue in order to verify the existence of the package */

            SignedTransaction result = proxy.startTrackedFlowDynamic(RegisterPkgFlow.DevInitiation.class,
                    wrapper.getName(), wrapper.getDescription(), wrapper.getVersion(), pkgInfoId,
                    wrapper.getImageLink(), wrapper.getPkgType(), wrapper.getPoPrice())
                    .getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("Transaction id " + result.getId() +
                    " committed to ledger.\n" + result.getTx().getOutput(0));
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "marketplace", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPkgs() {
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(proxy.startTrackedFlowDynamic(GetPkgsFlow.GetPkgsInfoInitiation.class)
                            .getReturnValue().get());
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping(value = "self-issue-cash", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> selfIssueCash(@RequestParam(value = "amount")int amount,
                                                @RequestParam(value = "currency")String currency) {
        try {
            Cash.State cashState = proxy.startTrackedFlowDynamic(SelfIssueCashFlow.class,
                    new Amount<>((long) amount * 100, Currency.getInstance(currency))).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(cashState.toString());
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "marketplace/buy-pkg/{linear-id}", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> buyPkg(@PathVariable("linear-id")String linearId) {
        return null;
    }
}