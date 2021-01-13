package it.nextworks.corda.webserver;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import it.nextworks.corda.flows.*;
import it.nextworks.corda.states.FeeAgreementState;
import it.nextworks.corda.states.PkgLicenseState;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.Money;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.contracts.asset.Cash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.nextworks.corda.webserver.ControllerUtils.*;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_SIZE;
import static net.corda.finance.workflows.GetBalances.getCashBalances;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@SuppressWarnings("unused")
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

    private static class RegisterPkgWrapper {

        @JsonProperty("name") private final String name;
        @JsonProperty("description") private final String description;
        @JsonProperty("version") private final String version;
        @JsonProperty("pkgInfoId") private final String pkgInfoId;
        @JsonProperty("imageLink") private final String imageLink;
        @JsonProperty("pkgType") private final PkgOfferState.PkgType pkgType;
        @JsonProperty("poPrice") private final ProductOfferingPrice poPrice;

        @JsonCreator
        public RegisterPkgWrapper(@JsonProperty("name")String name,
                                  @JsonProperty("description")String description,
                                  @JsonProperty("version")String version,
                                  @JsonProperty("pkgInfoId")String pkgInfoId,
                                  @JsonProperty("imageLink")String imageLink,
                                  @JsonProperty("pkgType")PkgOfferState.PkgType pkgType,
                                  @JsonProperty("poPrice")ProductOfferingPrice poPrice) {
            this.name        = name;
            this.description = description;
            this.version     = version;
            this.pkgInfoId   = pkgInfoId;
            this.imageLink   = imageLink;
            this.pkgType     = pkgType;
            this.poPrice     = poPrice;
        }

        /* Getters */

        public String getName() { return name; }

        public String getDescription() { return description; }

        public String getVersion() { return version; }

        public String getPkgInfoId() { return pkgInfoId; }

        public String getImageLink() { return imageLink; }

        public PkgOfferState.PkgType getPkgType() { return pkgType; }

        public ProductOfferingPrice getPoPrice() { return poPrice; }
    }

    private static class UpdatePkgWrapper {

        @JsonProperty("linearId") private final UniqueIdentifier linearId;
        @JsonProperty("name") private final String name;
        @JsonProperty("description") private final String description;
        @JsonProperty("version") private final String version;
        @JsonProperty("imageLink") private final String imageLink;
        @JsonProperty("poPrice") private final ProductOfferingPrice poPrice;

        @JsonCreator
        public UpdatePkgWrapper(@JsonProperty("linearId") UniqueIdentifier linearId,
                                @JsonProperty("name")String name,
                                @JsonProperty("description")String description,
                                @JsonProperty("version")String version,
                                @JsonProperty("imageLink")String imageLink,
                                @JsonProperty("poPrice")ProductOfferingPrice poPrice) {
            this.linearId    = linearId;
            this.name        = name;
            this.description = description;
            this.version     = version;
            this.imageLink   = imageLink;
            this.poPrice     = poPrice;
        }

        public UniqueIdentifier getLinearId() { return linearId; }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public String getVersion() { return version; }

        public String getImageLink() { return imageLink; }

        public ProductOfferingPrice getPoPrice() { return poPrice; }
    }

    private static class BuyPkgWrapper {

        @JsonProperty("linearId") private final UniqueIdentifier linearId;
        @JsonProperty("pkgInfoId") private final String pkgInfoId;
        @JsonProperty("pkgType") private final PkgOfferState.PkgType pkgType;
        @JsonProperty("price") private final Money price;

        @JsonCreator
        public BuyPkgWrapper(@JsonProperty("linearId") UniqueIdentifier linearId,
                             @JsonProperty("pkgInfoId")String pkgInfoId,
                             @JsonProperty("pkgType")PkgOfferState.PkgType pkgType,
                             @JsonProperty("price") Money price) {
            this.linearId  = linearId;
            this.pkgInfoId = pkgInfoId;
            this.pkgType   = pkgType;
            this.price     = price;
        }

        /* Getters */

        public UniqueIdentifier getLinearId() { return linearId; }

        public String getPkgInfoId() { return pkgInfoId; }

        public PkgOfferState.PkgType getPkgType() { return pkgType; }

        public Money getPrice() { return price; }
    }

    /* Helpers for filtering the network map cache */

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
    private String status() {
        logger.info(statusRequestOK);

        return "200";
    }

    @GetMapping(value = "servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() {
        String result = LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC")).toString();
        logger.info(serverTimeRequestOK);

        return result;
    }

    @GetMapping(value = "addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() {
        String result = proxy.nodeInfo().getAddresses().toString();
        logger.info(addressesRequestOK);

        return result;
    }

    @GetMapping(value = "identities", produces = TEXT_PLAIN_VALUE)
    private String identities() {
        String result = proxy.nodeInfo().getLegalIdentities().toString();
        logger.info(identitiesRequestOK);

        return result;
    }

    @GetMapping(value = "platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() {
        String result = Integer.toString(proxy.nodeInfo().getPlatformVersion());
        logger.info(platformVersionRequestOK);

        return result;
    }

    @GetMapping(value = "notaries", produces = TEXT_PLAIN_VALUE)
    private String notaries() {
        String result = proxy.notaryIdentities().toString();
        logger.info(notariesRequestOK);

        return result;
    }

    @GetMapping(value = "flows", produces = TEXT_PLAIN_VALUE)
    private String flows() {
        String result = proxy.registeredFlows().toString();
        logger.info(flowsRequestOK);

        return result;
    }

    @GetMapping(value = "me", produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami() {
        HashMap<String, String> map = new HashMap<>();
        map.put("me", me.toString());
        logger.info(meRequestOK);

        return map;
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
        logger.info(peersRequestOK);

        return map;
    }

    /* ####### Flows Calls ####### */

    @PutMapping(value = "establish-fee-agreement", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> establishFeeAgreement(@RequestParam(value = "maxAcceptableFee") int maxAcceptableFee) {
        try {
            proxy.startTrackedFlowDynamic(EstablishFeeAgreementFlow.DevInitiation.class, maxAcceptableFee)
                    .getReturnValue().get();
            logger.info(feeAgreementEstablished);

            return ResponseEntity.status(HttpStatus.CREATED).body(feeAgreementEstablished);
        } catch(Exception e) {
            logger.error(feeAgreementFailed + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage().substring(e.getMessage().lastIndexOf(':') + 1));
        }
    }

    private ResponseEntity<String> isOnBoarded(String pkgInfoId, PkgOfferState.PkgType pkgType) {
        if(pkgInfoId == null || pkgType == null) {
            logger.error(pkgRegisterFailed + nullParam);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(pkgRegisterFailed + nullParam);
        }

        String request;
        if(pkgType.equals(PkgOfferState.PkgType.VNF))
            request = catalogueURL + "vnfpkgm/v1/vnf_packages/";
        else
            request = catalogueURL + "nsd/v1/pnf_descriptors/";
        request += pkgInfoId;

        logger.info("GET " + request);

        try {
            URL url = new URL(request);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestProperty("Accept", "application/json");
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();

            if(responseCode != HttpURLConnection.HTTP_OK) {
                if(responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    logger.error(pkgRegisterFailed + onBoardingRequired);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(onBoardingRequired);
                }
                else {
                    logger.error(pkgRegisterFailed + errorWhileProcessingRq);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorWhileProcessingRq);
                }
            }

            logger.info(getRequestSucceed);
            return null;
        } catch(IOException ie) {
            logger.error(pkgRegisterFailed + errorWhileProcessingRq);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorWhileProcessingRq);
        }
    }

    @PutMapping(value = "register-pkg", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> registerPkg(@RequestBody RegisterPkgWrapper wrapper) {
        String pkgInfoId = wrapper.getPkgInfoId();
        PkgOfferState.PkgType pkgType = wrapper.getPkgType();

        ResponseEntity<String> res = isOnBoarded(pkgInfoId, pkgType);
        if(res != null)
            return res;

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(RegisterPkgFlow.DevInitiation.class,
                    wrapper.getName(), wrapper.getDescription(), wrapper.getVersion(), pkgInfoId,
                    wrapper.getImageLink(), pkgType, wrapper.getPoPrice()).getReturnValue().get();
            PkgOfferState pkgOfferState = result.getTx().outputsOfType(PkgOfferState.class).get(0);
            logger.info(pkgRegistered + pkgOfferState.getLinearId());

            return ResponseEntity.status(HttpStatus.CREATED).body(pkgRegistered + pkgOfferState.getLinearId());
        } catch(Exception e) {
            logger.error(pkgRegisterFailed + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping(value = "update-pkg", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> updatePkg(@RequestBody UpdatePkgWrapper wrapper) {
        UniqueIdentifier linearId = wrapper.getLinearId();
        if(linearId == null) {
            logger.error(pkgUpdateFailed + nullParam);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(pkgUpdateFailed + nullParam);
        }

        QueryCriteria.LinearStateQueryCriteria queryCriteria =
                new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(linearId.getId()),
                        null, Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<PkgOfferState>> lst = proxy.vaultQueryByCriteria(queryCriteria, PkgOfferState.class).getStates();
        if(lst.size() == 0) {
            logger.error(pkgUpdateFailed + notExistingPkg);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(notExistingPkg);
        }

        PkgOfferState oldPkgOfferState = lst.get(0).getState().getData();
        ResponseEntity<String> res = isOnBoarded(oldPkgOfferState.getPkgInfoId(), oldPkgOfferState.getPkgType());
        if(res != null)
            return res;

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(UpdatePkgFlow.DevInitiation.class,
                    wrapper.getLinearId(), wrapper.getName(), wrapper.getDescription(), wrapper.getVersion(),
                    wrapper.getImageLink(), wrapper.getPoPrice()).getReturnValue().get();
            PkgOfferState pkgOfferState = result.getTx().outputsOfType(PkgOfferState.class).get(0);
            logger.info(pkgUpdated + pkgOfferState.getLinearId());

            return ResponseEntity.status(HttpStatus.CREATED).body(pkgUpdated + pkgOfferState.getLinearId());
        } catch(Exception e) {
            logger.error(pkgUpdateFailed + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "marketplace", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPkgs() {
        try {
            List<PkgOfferState> result = proxy.startTrackedFlowDynamic(GetPkgsFlow.GetPkgsInfoInitiation.class)
                    .getReturnValue().get();
            logger.info(marketplaceRequestOK);

            return ResponseEntity.status(HttpStatus.OK).body(result);
        } catch(Exception e) {
            logger.error(pkgsGetFailed + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping(value = "marketplace-search", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFilteredPkgs(@RequestParam(value = "uuid", required = false)UUID uuid,
                                             @RequestParam(value = "name", required = false)String name,
                                             @RequestParam(value = "description", required = false)String description,
                                             @RequestParam(value = "version", required = false)String version,
                                             @RequestParam(value = "value", required = false)String value,
                                             @RequestParam(value = "unit", required = false)String unit) {
        GetPkgsFlowUtils.QueryBuilder queryBuilder = new GetPkgsFlowUtils.QueryBuilder()
                .setLinearId(uuid)
                .setName(name)
                .setDescription(description)
                .setVersion(version);

        if(value != null) {
            try {
                BigDecimal bigDecimalValue = new BigDecimal(value).setScale(2,
                        BigDecimal.ROUND_HALF_EVEN);
                queryBuilder.setValue(bigDecimalValue);
            } catch (NumberFormatException nfe) {
                logger.error(badRequestValue + nfe.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(badRequestValue);
            }
        }

        queryBuilder.setUnit(unit);

        try {
            List<PkgOfferState> result =
                    proxy.startTrackedFlowDynamic(GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation.class,
                            queryBuilder.build()).getReturnValue().get();
            logger.info(marketplaceRequestOK);

            return ResponseEntity.status(HttpStatus.OK).body(result);
        } catch(Exception e) {
            logger.error(pkgsGetFailed + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping(value = "self-issue-cash", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> selfIssueCash(@RequestParam(value = "amount")int amount,
                                                @RequestParam(value = "currency")String currency) {
        if (amount <= 0) {
            logger.error(cashIssueFailed + negativeAmount);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(negativeAmount);
        }
        try {
            Cash.State cashState = proxy.startTrackedFlowDynamic(SelfIssueCashFlow.class,
                    new Amount<>((long) amount * 100, Currency.getInstance(currency))).getReturnValue().get();
            logger.info(cashIssued);

            return ResponseEntity.status(HttpStatus.CREATED).body(cashState.toString());
        } catch(IllegalArgumentException iae){
            logger.error(cashIssueFailed + invalidISOCode);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(invalidISOCode);
        } catch(Exception e) {
            logger.error(cashIssueFailed + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage().substring(e.getMessage().lastIndexOf(':') + 1));
        }
    }

    @PostMapping(value = "marketplace/buy-pkg", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> buyPkg(@RequestBody BuyPkgWrapper wrapper) {
        ResponseEntity<String> res = isOnBoarded(wrapper.getPkgInfoId(), wrapper.getPkgType());
        if(res != null)
            return res;

        try {
            Money money = wrapper.getPrice();
            if(money == null) {
                logger.error(pkgPurchaseFailed + nullParam);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(pkgPurchaseFailed + nullParam);
            }

            Amount<Currency> price = Amount.fromDecimal(BigDecimal.valueOf(money.getValue()).setScale(2,
                    BigDecimal.ROUND_HALF_EVEN), Currency.getInstance(money.getUnit()));
            SignedTransaction result = proxy.startTrackedFlowDynamic(BuyPkgFlow.PkgBuyerInitiation.class,
                    wrapper.getLinearId(), price).getReturnValue().get();
            logger.info(pkgPurchased + wrapper.getLinearId());

            return ResponseEntity.status(HttpStatus.CREATED).body(pkgPurchased + wrapper.getLinearId());
        } catch(IllegalArgumentException iae) {
            logger.error(pkgPurchaseFailed + iae.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(iae.getMessage());
        } catch(Exception e) {
            logger.error(pkgPurchaseFailed + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping(value = "delete-pkg", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deletePkg(@RequestBody UniqueIdentifier linearId) {
        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(DeletePkgFlow.DevInitiation.class,
                    linearId).getReturnValue().get();
            logger.info(pkgDeleted + linearId);

            return ResponseEntity.status(HttpStatus.OK).body(pkgDeleted + linearId);
        } catch(Exception e) {
            logger.error(pkgDeleteFailed + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /* ####### Vault Queries ####### */

    @GetMapping(value = "cash-balances", produces = APPLICATION_JSON_VALUE)
    public Map<Currency, Amount<Currency>> cashBalances(){
        Map<Currency, Amount<Currency>> result = getCashBalances(proxy);
        logger.info(cashBalancesRequestOK);

        return result;
    }

    @GetMapping(value = "fee-agreement-state", produces = APPLICATION_JSON_VALUE)
    public FeeAgreementState getFeeAgreement() {
        FeeAgreementState result = proxy.vaultQuery(FeeAgreementState.class).getStates().get(0).getState().getData();
        logger.info(feeAgreementStateRequestOK);

        return result;
    }

    @GetMapping(value = "pkg-offer-state", produces = APPLICATION_JSON_VALUE)
    public List<PkgOfferState> getPkgOfferState() {
        int pageNumber = DEFAULT_PAGE_NUM;
        List<StateAndRef<PkgOfferState>> states = new ArrayList<>();
        long totalResults;
        do {
            PageSpecification pageSpecification = new PageSpecification(pageNumber, DEFAULT_PAGE_SIZE);
            Vault.Page<PkgOfferState> results =
                    proxy.vaultQueryByWithPagingSpec(PkgOfferState.class,
                            new QueryCriteria.VaultQueryCriteria()
                                    .withStatus(Vault.StateStatus.UNCONSUMED), pageSpecification);
            totalResults = results.getTotalStatesAvailable();
            states.addAll(results.getStates());
            pageNumber++;
        } while((DEFAULT_PAGE_SIZE * (pageNumber - 1) <= totalResults));

        List<PkgOfferState> pkgOfferStateList = new ArrayList<>();
        for(StateAndRef<PkgOfferState> pkgOfferStateAndRef : states)
            pkgOfferStateList.add(pkgOfferStateAndRef.getState().getData());

        logger.info(pkgOfferStateRequestOK);

        return pkgOfferStateList;
    }

    @GetMapping(value = "pkg-license-state", produces = APPLICATION_JSON_VALUE)
    public List<PkgLicenseState> getPkgLicenseState() {
        int pageNumber = DEFAULT_PAGE_NUM;
        List<StateAndRef<PkgLicenseState>> states = new ArrayList<>();
        long totalResults;
        do {
            PageSpecification pageSpecification = new PageSpecification(pageNumber, DEFAULT_PAGE_SIZE);
            Vault.Page<PkgLicenseState> results =
                    proxy.vaultQueryByWithPagingSpec(PkgLicenseState.class,
                            new QueryCriteria.VaultQueryCriteria()
                                    .withStatus(Vault.StateStatus.ALL), pageSpecification);
            totalResults = results.getTotalStatesAvailable();
            states.addAll(results.getStates());
            pageNumber++;
        } while((DEFAULT_PAGE_SIZE * (pageNumber - 1) <= totalResults));

        List<PkgLicenseState> pkgLicenseStateList = new ArrayList<>();
        for(StateAndRef<PkgLicenseState> pkgLicenseStateAndRef : states)
            pkgLicenseStateList.add(pkgLicenseStateAndRef.getState().getData());

        logger.info(pkgLicenseStateRequestOK);

        return pkgLicenseStateList;
    }
}