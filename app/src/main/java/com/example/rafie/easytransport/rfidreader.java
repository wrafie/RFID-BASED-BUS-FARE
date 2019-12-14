package com.example.rafie.easytransport;

import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.firebase.auth.*;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.lang.String;

public class rfidreader extends AppCompatActivity {
    public final String ACTION_USB_PERMISSION = "com.example.rafie.USB_PERMISSION";
    public static final String EXTRA_MESSAGE = "com.example.rafie.easytransport.MESSAGE";
    Button startButton, clearButton, stopButton;
    TextView textView;
    TextView rfid;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    String input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfidreader);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        textView = (TextView) findViewById(R.id.textView);
        rfid = (TextView)findViewById(R.id.rfidTag);
        setUiEnabled(false);
    }
    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);
        //rfid.setEnabled(bool);
    }
    public void onClickStart(View view) {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }
    @Override
    protected void onStart() {
        super.onStart();

        final UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
            @Override
            public void onReceivedData(byte[] arg0) {
                String data = null;
                try {
                    data = new String(arg0, "UTF-8");
                    tvAppend(textView, data);
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }
                finally {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            input = textView.getText().toString();
                            input = input.replaceAll("\\s","");
                            //rfid.setText(input);

                        }
                    });
                }

            }

        };
        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) {
                        connection = usbManager.openDevice(device);
                        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                        if (serialPort != null) {
                            if (serialPort.open()) {
                                setUiEnabled(true);
                                serialPort.setBaudRate(9600);
                                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                                serialPort.read(mCallback);
                            } else {
                                Log.d("SERIAL", "PORT NOT OPEN");
                            }
                        } else {
                            Log.d("SERIAL", "PORT IS NULL");
                        }
                    } else {
                        Log.d("SERIAL", "PERM NOT GRANTED");
                    }
                } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                    onClickStart(startButton);
                } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    onClickStop(stopButton);

                }
            }

            ;
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
    }

    public void onClickcard(View view) {
        setUiEnabled(false);
        serialPort.close();
        Intent intent = new Intent(this, onTravel.class);
        intent.putExtra(EXTRA_MESSAGE, input);
        startActivity(intent);
    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();

    }

    public void onClickClear(View view) {
        textView.setText("");
        rfid.setText("");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });

    }

}

