package com.parrot.freeflight.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Binder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import java.util.Date;


public class DroneAutonomyAVE extends Service {
    DroneControlService droneControlService;

    public BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final String TAG = "CommsActivity";
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    public String turn;
    public String direction;
    public String speed;
    private int lonSample = 0;
    private int latSample = 0;
    public Timer timer;
    public TimerTask timerTask;
    final Handler handler = new Handler();
    private long stopwatch;
    private int emptyReceiveCount = 0;

    // Constant for UUID
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //final String address = "B8:27:EB:7A:B9:13";
    public final static String EXTRA_ADDRESS = "B8:27:EB:7A:B9:13";

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.i("AMANDA","service connected");
            droneControlService = ((DroneControlService.LocalBinder) service).getService(); //represents the drone

            //TODO: add Jess' service stuff in here
            //i.e. communicationsService = ((BTService.myBinder) service).getService();     //represents comms

            //try and catch statements are going to have if statements inside:
            // if (variable taken from comms = right forward)
            //    then turn that into droneControlService.'command'
/*            try {                                           //inside should be the comms variables that kayla parsed
                Log.i("AMANDA","I'm trying!!");
                droneControlService.triggerTakeOff();
                Thread.sleep(5000);
                droneControlService.setYaw(0.5f);
                Thread.sleep(2000);
                droneControlService.setYaw(-0.3f);
                Thread.sleep(2000);
                droneControlService.setGaz(0.3f);
                Thread.sleep(2500);
                droneControlService.triggerTakeOff();
            } catch(Exception e) {
                e.printStackTrace();
            }*/


        }


        public void onServiceDisconnected(ComponentName name)
        {
            droneControlService = null;
            try {
                mmSocket.close();
            } catch (IOException closeException) {

            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        bindService(new Intent(this, DroneControlService.class), mConnection, Context.BIND_AUTO_CREATE);
        final String address = "B8:27:EB:7A:B9:13";
        final BluetoothDevice device = BTAdapter.getRemoteDevice(address);
        try {
            new ConnectThread(device).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO: get Jess' service in here and make sure we can connect to raspberry pi still
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    public class ConnectThread extends Thread {
        private ConnectThread(BluetoothDevice device) throws IOException {
            /*if (mmSocket != null) {
                if(mmSocket.isConnected()) {
                    send();
                }
            }*/
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                // This is the UUID that is in the Python Code on the RP3
                UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
                tmp = mmDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            BTAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.v(TAG, "Connection exception!");
                try {
                    mmSocket.close();
                   /* mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                    mmSocket.connect();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                */
                } catch (IOException closeException) {

                }
            }

            receive();
        }

        // InputStream: accepts an input stream of bytes
        // getInputStream(): called in order to retreive InputStream objects, which
        // are automatically connected to the socket
        public void receive() throws IOException {

            InputStream mmInputStream = mmSocket.getInputStream();
            byte[] buffer = new byte[1024];
            //int bytes = 0;

            try {
                long startTime = System.currentTimeMillis();
                //long stopwatch;
                emptyReceiveCount = 0;
                while (emptyReceiveCount < 5) {
                    // Sample rate = 250 ms = .25 second
                    stopwatch = (new Date()).getTime() - startTime;
                    if (stopwatch == 250) {
                        // SEND DATA
                        String msg = dataToSend();
                        OutputStream mmOutputStream = mmSocket.getOutputStream();
                        // writes bytes from the specified byte array to this output stream
                        mmOutputStream.write(msg.getBytes());


                        // RECEIVE DATA
                        // Try to receive message
                        if (mmInputStream.available() != 0) {
                            int bytes = mmInputStream.read(buffer);
                            String readMessage = new String(buffer, 0, bytes);
                            Log.d(TAG, "Receive);d from CCP (Unparsed): " + readMessage);
                            parseDriveCommand(readMessage);
                        }

                        // if message wasn't received, increment emptyReceiveCount
                        else {
                            emptyReceiveCount++;
                        }

                        startTime = System.currentTimeMillis();
                    }
                }

                // NEED TO LAND, WE HAVE NOT GOTTEN MESSAGES IN 1 SEC

                /*String readMessage = "";

                // continue until no byte is available because the stream is at the end of the file
                while (readMessage != "End") {


                    int bytes = mmInputStream.read(buffer);
                    readMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "Receive);d from CCP (Unparsed): " + readMessage);
                    parseDriveCommand(readMessage);
*//*                    String sendMsg = dataToSend();
                    send(sendMsg);*//*
                }
                // Sets TextView object to display the message
//                TextView test1 = (TextView) findViewById(R.id.Voltage);
//                test1.setText("Data from CCP\n" + readMessage);
                //mmSocket.close();*/
            } catch (IOException e) {
                Log.e(TAG, "Problems occurred!");
                return;
            }
        }
    }
    public void parseDriveCommand(String message){
        // turn, direction, speed, stuff for PTP
        String string = "Straight,Forward,27,36,-2";

        String[] values = string.split(",");
        turn = values[0];
        direction = values[1];
        speed = values[2];

        Log.d(TAG, "Received turn: " + turn);
        Log.d(TAG, "Received turn: " + direction);
        Log.d(TAG, "Received turn: " + speed);

    }

    public String dataToSend() {
        latSample ++;
        lonSample+= 4;
        String st = Long.toString(stopwatch);
        if (emptyReceiveCount == 3) {
            String data = st  + " " + "Receive Count is 3";
            return data;
        }
        String data = st  + " " + Integer.toString(emptyReceiveCount);
        return data;

    }

}

