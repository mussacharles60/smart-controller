package com.myssa.smartcontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothHelper {

    private static final String TAG = "BluetoothHelper";
    private final static int CONNECTION_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ_STATUS = 2; // used in bluetooth handler to identify message update
    private final Context context;
    private final BluetoothHelperListener listener;
    private String address;
    private Handler handler;
    private BluetoothSocket mmSocket;
    private ConnectedThread connectedThread;
    private CreateConnectThread createConnectThread;

    public BluetoothHelper(Context context, String address, BluetoothHelperListener listener) {
        this.context = context;
        this.address = address;
        this.listener = listener;
        this.init();
    }

    public static boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    public static void enableBluetooth(AppCompatActivity activity) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // check api level if >= 31 check self permission for location and bluetooth. If not granted, request permission
            // else just enable bluetooth
            if (canAccessBluetooth(activity)) {
                activity.startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
            }
        }
    }

    @SuppressLint("InlinedApi")
    public static boolean canAccessBluetooth(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // ActivityCompat.requestPermissions((MainActivity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT}, MainActivity.PERMISSION_REQUEST_CODE);
            return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void setAddressAndInitialize(String address) {
        this.address = address;
        this.stop();
        new android.os.Handler(Looper.getMainLooper()).postDelayed(this::init, 1000);
    }

    @SuppressLint("HandlerLeak")
    private void init() {
        if (address == null) {
            listener.onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
            return;
        }

        listener.onConnectionStatusChanged(ConnectionStatus.CONNECTING);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createConnectThread = new CreateConnectThread(bluetoothAdapter, address);
        createConnectThread.start();

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case CONNECTION_STATUS:
                        switch (msg.arg1) {
                            case 1:
                                listener.onConnectionStatusChanged(ConnectionStatus.CONNECTED);
                                break;
                            case -1:
                                listener.onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
                                break;
                        }
                        break;
                    case MESSAGE_READ_STATUS:
                        String deviceMsg = msg.obj.toString(); // Read message from Device
                        listener.onMessageReceived(deviceMsg);
                        //switch (deviceMsg.toLowerCase()) {
                        //    case "led is turned on":
                        //        break;
                        //    case "led is turned off":
                        //        break;
                        //}
                        break;
                }
            }
        };
    }

    public void send(String message) {
        Log.d(TAG, "send: connectedThread: " + message);
        if (connectedThread == null) {
            Log.e(TAG, "send: error: connectedThread: null");
            if (listener != null) {
                listener.onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
            }
            return;
        }
        Log.d(TAG, "send: " + message);
        connectedThread.write(message);
    }

    public void stop() {
        Log.d(TAG, "stop: ");
        try {
            if (createConnectThread != null) {
                createConnectThread.cancel();
                createConnectThread = null;
            }
            if (connectedThread != null) {
                connectedThread.cancel();
                connectedThread = null;
            }
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
                handler = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "stop: ", e);
            e.printStackTrace();
        }
        if (listener != null) {
            listener.onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
        }
    }

    public enum ConnectionStatus {
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }

    public interface BluetoothHelperListener {
        void onConnectionStatusChanged(ConnectionStatus status);

        void onMessageReceived(String message);
    }

    public interface ScannerListener {
        void onAdapterEnabled();

        void onAdapterDisabled();

        void onDevicesFound(List<Scanner.Device> devices);
    }

    public static class Scanner {

        private final Context context;
        private final ScannerListener listener;
        private BluetoothAdapter adapter;

        public Scanner(Context context, ScannerListener listener) {
            this.context = context;
            this.listener = listener;
            this.init();
        }

        @SuppressLint("MissingPermission")
        private void init() {
            adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) { // Device doesn't support Bluetooth
                listener.onAdapterDisabled();
                return;
            }
            if (!adapter.isEnabled()) {
                listener.onAdapterDisabled();
                if (canAccessBluetooth(context)) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    context.startActivity(enableBtIntent);
                }
                return;
            }
            listener.onAdapterEnabled();
        }

        @SuppressLint("MissingPermission")
        public void scanDevices() {
            if (adapter == null) { // Device doesn't support Bluetooth
                listener.onAdapterDisabled();
                return;
            }
            if (!adapter.isEnabled()) {
                listener.onAdapterDisabled();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (!canAccessBluetooth(context)) {
                    listener.onAdapterDisabled();
                    return;
                }
                context.startActivity(enableBtIntent);
            }

            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                List<Device> devices = new ArrayList<>();
                for (BluetoothDevice device : pairedDevices) {
                    // Log.d(TAG, "scanDevices: " + device.getName() + " " + device.getAddress());
                    devices.add(new Device(device.getName(), device.getAddress()));
                }
                listener.onDevicesFound(devices);
            }
        }

        public void stop() {
            try {
                if (adapter != null) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        adapter.cancelDiscovery();
                    }
                    adapter = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "stop: ", e);
                e.printStackTrace();
            }
        }

        public static class Device {
            private final String name;
            private final String address;

            public Device(String name, String address) {
                this.name = name;
                this.address = address;
            }

            public String getName() {
                return name;
            }

            public String getAddress() {
                return address;
            }
        }
    }

    public class CreateConnectThread extends Thread {

        @SuppressLint("MissingPermission")
        public CreateConnectThread(@NonNull BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            if (!canAccessBluetooth(context)) {
                Log.e(TAG, "CreateConnectThread: BluetoothSocket: error: permission");
                listener.onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
                return;
            }
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!canAccessBluetooth(context)) {
                Log.e(TAG, "CreateConnectThread: run: error: permission");
                listener.onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
                return;
            }
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.d(TAG, "Status: Device connected");
                handler.obtainMessage(CONNECTION_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e(TAG, "Status: Cannot connect to device");
                    handler.obtainMessage(CONNECTION_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final Handler readHandler = new Handler(Looper.getMainLooper());
        private int bytes = 0;
        private String message;
        private final Runnable readRunner = () -> {
            if (message != null && message.length() > 0) {
                // Log.e(TAG, "Device Message: " + message);
                if (!message.endsWith("}")) {
                    message += "}";
                }
                handler.obtainMessage(MESSAGE_READ_STATUS, message).sendToTarget();
                message = null;
                bytes = 0;
            }
        };

        public ConnectedThread(@NonNull BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            //BufferedReader reader = new BufferedReader(new InputStreamReader(mmInStream, StandardCharsets.UTF_8));
            //Log.d(TAG, "run: reader: " + reader);
            //
            ////String line;
            ////StringBuilder message = new StringBuilder();
            ////while (true) {
            ////    try {
            ////        if ((line = reader.readLine()) == null)
            ////            break;
            ////    } catch (IOException e) {
            ////        Log.e(TAG, "Input stream was disconnected", e);
            ////        e.printStackTrace();
            ////        break;
            ////    }
            ////    // buffer.append(line);
            ////    message.append(line);
            ////}
            ////System.out.println(message);
            //
            //char[] buffer = new char[4096];
            //StringBuilder builder = new StringBuilder();
            //int numChars;
            //
            //while (true) {
            //    try {
            //        if (!((numChars = reader.read(buffer)) >= 0))
            //            break;
            //    } catch (IOException e) {
            //        Log.e(TAG, "Input stream was disconnected", e);
            //        e.printStackTrace();
            //        break;
            //    }
            //    builder.append(buffer, 0, numChars);
            //}
            //String msg = builder.toString();
            //System.out.println(msg);
            //
            //if (msg.length() > 0) {
            //    Log.e(TAG, "Device Message: " + msg);
            //    handler.obtainMessage(MESSAGE_READ_STATUS, msg).sendToTarget();
            //}

            byte[] buffer = new byte[1024];  // buffer store for the stream
            bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from device until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    // Log.d(TAG, "input buffer: " + (new String(buffer, 0, bytes)));
                    message = new String(buffer, 0, bytes);
                    if (message.length() > 0) {
                        readHandler.removeCallbacks(readRunner);
                        readHandler.postDelayed(readRunner, 250);
                    }
                    bytes++;
                    //if (buffer[bytes] == '\n') {
                    //    readMessage = new String(buffer, 0, bytes);
                    //    Log.e(TAG, "Device Message: " + readMessage);
                    //    handler.obtainMessage(MESSAGE_READ_STATUS, readMessage).sendToTarget();
                    //    bytes = 0;
                    //} else {
                    //    bytes++;
                    //}
                } catch (IOException e) {
                    readHandler.removeCallbacks(readRunner);
                    Log.e(TAG, "Input stream was disconnected", e);
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this to send data to the remote device */
        public void write(@NonNull String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Send Error, Unable to send message", e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}
