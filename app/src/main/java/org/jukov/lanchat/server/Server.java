package org.jukov.lanchat.server;

import android.content.Context;
import android.content.Intent;

import org.jukov.lanchat.network.TCP;
import org.jukov.lanchat.network.UDP;
import org.jukov.lanchat.util.BroadcastStrings;
import org.jukov.lanchat.util.IntentStrings;
import org.jukov.lanchat.util.NetworkUtils;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by jukov on 05.02.2016.
 */
public class Server extends Thread implements Closeable {

    private Context context;
    private int port;
    private Set<ClientConnection> clientConnections;
    private ExecutorService executorService;
    private TCP tcp;

    private boolean stopBroadcastFlag;

    public Server(int port, final Context context) {
        this.context = context;
        this.port = port;
        stopBroadcastFlag = false;
        clientConnections = Collections.synchronizedSet(new HashSet<ClientConnection>());
        executorService = Executors.newFixedThreadPool(10);

        tcp = new TCP(port, new TCP.ClientListener() {
            @Override
            public void onReceive(Socket socket) {
                ClientConnection clientConnection = new ClientConnection(socket, getServer());
                executorService.execute(clientConnection);
                clientConnections.add(clientConnection);
                Intent intent = new Intent(IntentStrings.BROADCAST_ACTION);
                intent.putExtra(IntentStrings.EXTRA_TYPE, IntentStrings.TYPE_DEBUG);
                intent.putExtra(IntentStrings.EXTRA_DEBUG, "Mode: server; clients - " + clientConnections.size());
                context.sendBroadcast(intent);
            }
        });
        tcp.start();
    }

    @Override
    public void run() {
        try {
            InetAddress broadcastAddress = NetworkUtils.getBroadcastAddress(context);
            while (!stopBroadcastFlag) {
                UDP.send(port, broadcastAddress, BroadcastStrings.SERVER_BROADCAST);
                TimeUnit.MILLISECONDS.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        stopBroadcastFlag = true;
    }

    public Server getServer() {
        return this;
    }

    public void broadcastMessage(String message) {
        for (ClientConnection clientConnection: clientConnections) {
            clientConnection.sendMessage(message);
        }
    }
}