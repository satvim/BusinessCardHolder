package com.cognitiveapplabs.businesscardmaker;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by saanton on 9/28/2016.
 */
public class ConnectThread extends Thread {
    private static UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    public transient Context context;

    public ConnectThread(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        //BluetoothSocket tmp = null;
        this.mmDevice = device;
        this.context = CardActivity.getContext();
        
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string
            //mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) { }
        //mmSocket = tmp;
    }

    public void connect() {
        try {
            Log.i("BusinessCardMaker","Connecting to socket...");
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
            //Thread.sleep(5000);
            Log.i("BusinessCardMaker", "Connected");
        } catch (Exception connectException) {
            Log.e("BusinessCardMaker", connectException.toString());
            // Unable to connect try fallback
            try {
                Log.i("BusinessCardMaker","Trying fallback...");
                mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,2);
                mmSocket.connect();
                //Thread.sleep(5000);
                Log.i("BusinessCardMaker", "Connected");
            } catch (Exception fallbackException) {
                Log.e("BusinessCardMaker","Couldn't establish Bluetooth connection!");
                try {
                    mmSocket.close();
                } catch (IOException e3) {
                    Log.e("BusinessCardMaker", "unable to close() " + mmSocket.toString() + " socket during connection failure", e3);
                }
                Log.v("BusinessCardMaker","Outside .close() catch");
                return;
            }
            //Log.v("BusinessCardMaker","Outside fallback catch");
        }
        //Log.v("BusinessCardMaker", "Outside main Catch");
        //sendBusinessCard(mmSocket);
        CardActivity card = new CardActivity();
            try {
                CardActivity.SendData sendData = card.new SendData();
                sendData.sendMessage(mmSocket);
            } catch (FileNotFoundException e) {
                Log.v("BusinessCardMaker", e.toString());
            }

    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
