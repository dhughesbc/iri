package com.iota.iri.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iota.iri.Neighbor;
import com.iota.iri.service.Node;

public class ReplicatorSourceProcessor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ReplicatorSourceProcessor.class);

    private Socket connection;

    final static int TRANSACTION_PACKET_SIZE = 10;

    private volatile boolean shutdown = false;

    public ReplicatorSourceProcessor(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        int count;
        byte[] data = new byte[2000];
        int offset = 0;
        boolean knownNeighbor = false;

        Neighbor neighbor = null;

        try {
            SocketAddress address = connection.getRemoteSocketAddress();
            InetSocketAddress inet_address = (InetSocketAddress) address;
            
            String uriString = "tcp://" + inet_address.getAddress().getHostAddress();
            final URI uri = new URI(uriString);
            final Neighbor fresh_neighbor = new Neighbor(
                    new InetSocketAddress(uri.getHost(), ReplicatorSourcePool.REPLICATOR_PORT));
            if (!Replicator.instance().getNeighbors().contains(fresh_neighbor)) {
                StringBuffer sb = new StringBuffer(80);
                sb.append("Got connected from unknown neighbor tcp://")
                    .append(inet_address.getHostName())
                    .append(":")
                    .append(String.valueOf(inet_address.getPort()))
                    .append(" (")
                    .append(inet_address.getAddress().getHostAddress())
                    .append(")");
                log.info(sb.toString());
                Node.instance().getNeighbors().add(fresh_neighbor);
                fresh_neighbor.setSource(connection);
            } else {
                log.info("Got connected from configured neighbor {}", inet_address.getAddress().getHostAddress());
                neighbor = Replicator.instance().getNeighborByAddress(inet_address);
            }
            
            ReplicatorSinkPool.instance().createSink(neighbor);

            InputStream stream = connection.getInputStream();
            while (!shutdown) {
                while (((count = stream.read(data, offset, TRANSACTION_PACKET_SIZE - offset)) != -1)
                        && (offset < TRANSACTION_PACKET_SIZE)) {
                    log.info("received {} bytes", count);
                    offset += count;
                }
                if (count == -1)
                    break;
                log.info("offset = {}", offset);
                offset = 0;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error("Could not read input ", e);
        } catch (URISyntaxException e) {
            log.error("URI syntax error ", e);
        } finally {
            try {
                log.info("session closed");                
                connection.close();
                neighbor.setSource(null);
            } catch (IOException ex) {
                log.error("Could not close connection", ex);
            }
        }
    }
}
