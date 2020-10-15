package it.nextworks.corda.webserver;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Wraps an RPC connection to a Corda node.
 * The RPC connection is configured using command line arguments:
 * - The host of the node we are connecting to
 * - The RPC port of the node we are connecting to
 * - The username for logging into the RPC client
 * - The password for logging into the RPC client
 */
@Component
public class NodeRPCConnection implements AutoCloseable {

    @Value("${config.rpc.host}")
    private String host;
    @Value("${config.rpc.username}")
    private String username;
    @Value("${config.rpc.password}")
    private String password;
    @Value("${config.rpc.port}")
    private int rpcPort;

    private CordaRPCConnection rpcConnection;
    private CordaRPCOps proxy;

    @PostConstruct
    public void initialiseNodeRPCConnection() {
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, rpcPort);
        CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
        rpcConnection = rpcClient.start(username, password);
        proxy = rpcConnection.getProxy();
    }

    public CordaRPCOps getProxy() { return proxy; }

    @PreDestroy
    @Override
    public void close () throws Exception { rpcConnection.notifyServerAndClose(); }
}