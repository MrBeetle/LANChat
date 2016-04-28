package org.jukov.lanchat.client;

import android.content.Context;
import android.util.Log;

import org.jukov.lanchat.db.DBHelper;
import org.jukov.lanchat.dto.ChatData;
import org.jukov.lanchat.dto.MessagingData;
import org.jukov.lanchat.dto.PeopleData;
import org.jukov.lanchat.dto.RoomData;
import org.jukov.lanchat.dto.ServiceData;
import org.jukov.lanchat.service.ServiceHelper;
import org.jukov.lanchat.util.JSONConverter;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.AbstractCollection;

import static org.jukov.lanchat.dto.ServiceData.MessageType;

/**
 * Created by jukov on 06.02.2016.
 */
public class Client extends Thread implements Closeable {

    private Context context;
//    private int port;
//    private String remoteIp;
    private PeopleData peopleData;

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private DBHelper dbHelper;

    public Client(Context context, String ip, int port) {
        this.context = context;
//        this.port = port;
//        this.remoteIp = remoteIp;
        peopleData = new PeopleData(context, PeopleData.ACTION_NONE);
        while (socket == null) {
            try {
                socket = new Socket(ip, port);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                peopleData.setAction(PeopleData.ACTION_CONNECT);
                sendMessage(JSONConverter.toJSON(peopleData));
                peopleData.setAction(PeopleData.ACTION_NONE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dbHelper = DBHelper.getInstance(context);
    }

    public void changeName(String name) {
        peopleData.setName(name);
        peopleData.setAction(PeopleData.ACTION_CHANGE_NAME);
        try {
            sendMessage(JSONConverter.toJSON(peopleData));
        } catch (IOException e) {
            e.printStackTrace();
        }
        peopleData.setAction(PeopleData.ACTION_NONE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        Log.d(getClass().getSimpleName(), "Client started");
        try {
            while (!socket.isClosed()) {
                String message = dataInputStream.readUTF();
                Log.d(getClass().getSimpleName(), message);
                Object data = JSONConverter.toPOJO(message);
                if (data instanceof ChatData) {
                    ChatData chatData = (ChatData) data;
                    dbHelper.insertMessage(chatData);
                    ServiceHelper.receiveMessage(context, chatData);

                } else if (data instanceof PeopleData) {
                    PeopleData peopleData = (PeopleData) data;
                    dbHelper.insertOrRenamePeople(peopleData);
                    ServiceHelper.receivePeople(context, peopleData);

                } else if (data instanceof RoomData) {
                    RoomData roomData = (RoomData) data;
                    dbHelper.insertOrRenameRoom(roomData);
                    ServiceHelper.receiveRoom(context, roomData);

                } else if (data instanceof AbstractCollection) {
                    Log.d(getClass().getSimpleName(), "receive AbstractCollection");
                    AbstractCollection dataBundle = (AbstractCollection) data;
                    MessagingData messagingData = (MessagingData) dataBundle.iterator().next();
                    if (messagingData instanceof ChatData) {
                        dbHelper.insertMessages(dataBundle);
                        ServiceHelper.receivePublicMessages(context, dataBundle);
                    } else if (messagingData instanceof RoomData) {
                        dbHelper.insertRooms(dataBundle);
                        ServiceHelper.receiveRooms(context, dataBundle);
                    }

                } else if (data instanceof ServiceData) {
                    Log.d(getClass().getSimpleName(), "Receive ServiceData");
                    ServiceData serviceData = (ServiceData) data;
                    if (serviceData.getMessageType() == MessageType.DELEGATION_SERVER_STATUS) {
                        close();
                        ServiceHelper.clearPeopleList(context);
                        ServiceHelper.startServer(context);
                        return;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
        * If action = ACTION_DISCONNECT, app closed
        * */
        if (peopleData.getAction() != PeopleData.ACTION_DISCONNECT) {
            close();
            ServiceHelper.clearPeopleList(context);
            ServiceHelper.searchServer(context);
        }
    }

    @Override
    public void close() {
        try {
            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
            Log.d(getClass().getSimpleName(), "close");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendDisconnect() {
        peopleData.setAction(PeopleData.ACTION_DISCONNECT);
        try {
            sendMessage(JSONConverter.toJSON(peopleData));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            Log.d(getClass().getSimpleName(), "In sendMessage()");
            dataOutputStream.writeUTF(message);
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLocalIP() {
        return socket.getLocalAddress().toString();
    }

    public void updateStatus() {
        ServiceHelper.updateStatus(context, "Mode: client");
    }


}
