package com.example.nguyenngocsonc.myapplication.screens.main;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.nguyenngocsonc.myapplication.R;
import com.example.nguyenngocsonc.myapplication.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;
    private static final String TAG = "ClientActivity";
    private Map<String, BluetoothDevice> mScanResults;
    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mLogHandler;
    private BluetoothGatt mGatt;
    private boolean mConnected;
    private Handler mHandler;
    private TextView textView;
    private ProgressBar progressBar;
    private ListView listView;
    private boolean mEchoInitialized;

    private ActivityMainBinding mBinding;
    private ArrayAdapter<String> listDevice;
    private EditText textInput;

    private TextView textChat;

    private BluetoothGatt gat;
    private BluetoothGattService ser;
    private BluetoothGattCharacteristic characteristic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
//        MainViewModel mainViewModel = new MainViewModel();
//        binding.setViewModel(mainViewModel);
        mLogHandler = new Handler(Looper.getMainLooper());
//        setContentView(R.layout.activity_main);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        textView = findViewById(R.id.list_item);
        textChat = findViewById(R.id.text_chat);
        progressBar = findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.GONE);

        listDevice = new ArrayAdapter<String>(this, R.layout.activity_main);

        String deviceInfo = "Device Info"
                + "\nName: " + mBluetoothAdapter.getName()
                + "\nAddress: " + mBluetoothAdapter.getAddress();

        mBinding.setClientDeviceInfoTextView(deviceInfo);
    }

    public void onStartScanning(View view) {
        boolean on = ((ToggleButton) view).isChecked();

        if (on) {
            startScan();
        } else {
            stopScan();
//            mBluetoothAdapter.disable();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
        }
    }

    private void startScan() {
//        if (!hasPermissions() || mScanning) {
//            return;
//        }
//        else {
//            requestBluetoothEnable();
//        }

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } else {
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }


        progressBar.setVisibility(View.VISIBLE);

        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

//        ScanFilter filter = new ScanFilter.Builder()
//                .setServiceUuid(new ParcelUuid(UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")))
//                .build();
//

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString("EEE7950D-73F1-4D4D-8E47-C090502DBD63")))
                .build();

        filters.add(filter);

        mScanResults = new HashMap<>();
//        mScanCallback = new BtleScanCallback(mScanResults);

        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                addScanResult(result);
                textView.setText(result.getDevice().getName());
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) {
                    addScanResult(result);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.d(TAG, "BLE Scan Failed with code " + errorCode);
            }

            private void addScanResult(ScanResult result) {
                Log.d("SonBD", "addScanResult " + result.getDevice().getName());
                BluetoothDevice device = result.getDevice();
                String deviceAddress = device.getAddress();
                mScanResults.put(deviceAddress, device);
                listDevice.add(deviceAddress);
                stopScan();
                connectDevice(device);

                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_stat_ac_unit)
                        .setContentTitle("Bluetooth Alert!")
                        .setContentText("Connected!");
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG);

            }
        };

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
        mScanning = true;
        Log.d(TAG, "startScan: ");

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, 10000);
    }

    private void stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            scanComplete();
        }

        mScanCallback = null;
        mScanning = false;
        mHandler = null;
        progressBar.setVisibility(View.GONE);
//        listView.setAdapter(listDevice);
    }

    private void scanComplete() {
        if (mScanResults.isEmpty()) {
            return;
        }
        for (String deviceAddress : mScanResults.keySet()) {
            Log.d(TAG, "Found device: " + deviceAddress);
        }
    }

    private boolean hasPermissions() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
            return false;
        } else if (!hasLocationPermissions()) {
            requestLocationPermission();
            return false;
        }
        return true;
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.d(TAG, "Requested user enables Bluetooth. Try starting the scan again.");
    }

    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
    }

    private class BtleScanCallback extends ScanCallback {

        BtleScanCallback(Map<String, BluetoothDevice> scanResults) {
            mScanResults = scanResults;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "BLE Scan Failed with code " + errorCode);
        }

        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();
            mScanResults.put(deviceAddress, device);
            Toast.makeText(MainActivity.this, result.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connectDevice(BluetoothDevice device) {
        Log.d("SonBD", "connectDevice " + device.getAddress());
        GattClientCallback gattClientCallback = new GattClientCallback();
        mGatt = device.connectGatt(this, false, gattClientCallback);
    }

    public void onSendMessage(View view) {
        textInput = findViewById(R.id.text_input);
        String t = textInput.getText().toString();

        BluetoothGattService service = gat.getService(UUID.fromString("00001805-0000-1000-8000-00805f9b34fb"));
        BluetoothGattCharacteristic chart = service.getCharacteristic(UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb"));

        chart.setValue(t.getBytes());
        gat.writeCharacteristic(chart);
        textInput.setText("");
        Log.d("SonBD", "onSendMessage: " + t);
        textChat.setText(textChat.getText() + "\n" + t);
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("SonBD", "onConnectionStateChange " + status);
            if (status == BluetoothGatt.GATT_FAILURE) {
                //disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                //disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //disconnectGattServer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d("SonBD", "onServicesDiscovered " + status);
            //if (status != BluetoothGatt.GATT_SUCCESS) {
                //return;
            //}
            gat = gatt;

            BluetoothGattService service = gatt.getService(UUID.fromString("00001805-0000-1000-8000-00805f9b34fb"));
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb"));

            gatt.readCharacteristic(characteristic);
//            Log.d("SonBD", "start read characteristic: " + characteristic.getValue());


                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                enableCharacteristicNotification(gatt, characteristic);
            Log.d("SonBD", "enableCharacteristicNotification ");

            //gat = gatt;
            //ser = service;
//            chart = characteristic;

//            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//
//            final int prop = characteristic.getProperties();
//
//            if ((prop | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
//                String text = "Hello";
//                characteristic.setValue(text.getBytes());
//
//                gatt.writeCharacteristic(characteristic);
//            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead: " + characteristic.getValue().toString());
            Log.d("SonBD", "start read characteristic: " + status + ", " + new String(characteristic.getValue()));
            textChat.setText(new String(characteristic.getValue()));
            textChat.setText(textChat.getText() + "\n" + new String(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
//            Log.d(TAG, "onCharacteristicWrite: " + status);
            Log.d("SonBD", "onCharacteristicWrite: " + new String(characteristic.getValue()));
//            textChat.setText(textChat.getText() + "\n" + new String(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d("SonBD", "onCharacteristicChanged: " + characteristic);
            textChat.setText(textChat.getText() + "\n" + new String(characteristic.getValue()));
        }
    }

    public void disconnectGattServer() {
        mConnected = false;
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    private void enableCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        boolean characteristicWriteSuccess = gatt.setCharacteristicNotification(characteristic, true);
        if (characteristicWriteSuccess) {
            Log.d("SonBD", "enableCharacteristicNotification: " + characteristic.getUuid().toString());
        }
    }
}
