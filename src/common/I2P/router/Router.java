package common.I2P.router;

import common.I2P.tunnels.Tunnel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Class represents Core Router module of I2P
 */
public class Router {
    /**
     * Map of current inbound Tunnels, key is Tunnel ID and Value is appropriate Tunnel
     */
    ConcurrentHashMap<Integer, Tunnel> inboundTunnels = new ConcurrentHashMap<>();
    /**
     * Map of current outbound Tunnels, key is Tunnel ID and Value is appropriate Tunnel
     */
    ConcurrentHashMap<Integer, Tunnel> outboundTunnels = new ConcurrentHashMap<>();
}
