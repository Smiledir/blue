package blue.oem.com.blue;


import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String TAG = "TAG";

    private TextView tStatus;
    private Boolean bSearch;
    private ListView lResult;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> BTArrayAdapter;
    private ArrayList<Object> BTResultMac;
    private HashSet<BluetoothDevice> BTDevices;
    private CountDownTimer scanTimer;
    private BluetoothSocket btSocket;
    private OutputStream os;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private boolean mBluetoothConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bSearch = false;
        tStatus = (TextView) findViewById(R.id.tStatus);
        lResult = (ListView) findViewById(R.id.resultListView);

        mBluetoothConnect = false;

        CreateBluetoothConnection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    private void CreateBluetoothConnection(){
        if(mBluetoothAdapter == null) {
            bSearch = false;
            tStatus.setText("Status: not supported");
            return;
        } else {
            if(!mBluetoothAdapter.isEnabled())
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
            tStatus.setText("Status: BlueTooth Enabled");
            BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            BTResultMac = new ArrayList<>();
            BTDevices = new HashSet<>();
            lResult.setAdapter(BTArrayAdapter);
        }

        lResult.setOnItemClickListener(new OnBluetoothDeviceClickListener());
    }

    private class OnBluetoothDeviceClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice((String) BTResultMac.get(pos));
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
                bSearch = true;
                scanTimer.cancel();
            }

            try {
                btSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.d("Exception", "socket create failed: " + e.getMessage() + ".");
                tStatus.setText("Socket create failed: " + e.getMessage() + ".");
            }

            tStatus.setText("Status: Connecting...");
            try {
                btSocket.connect();
            } catch (IOException e) {
                e.printStackTrace(System.err);
                Log.d("Exception", "Connection failure.");
                tStatus.setText("Status: Connecting Failed");
                mBluetoothConnect = false;
                return;
            }
            tStatus.setText("Status: Connecting finished " + BTResultMac.get(pos));
            try {
                os = btSocket.getOutputStream();
                mBluetoothConnect = true;
            } catch (IOException e) {
                Log.e("Exception", "Unable to get output stream of bluetooth socket");
                e.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != 100) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
    }


    private boolean checkBluetooth(){

        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothConnect;
    }

    private void sendBluetoothMessage(String str){
        if(checkBluetooth()) {
            try {
                os.write(str.getBytes());
            } catch (IOException e) {
                Log.e("Exception", "Failed to write messages");
                e.printStackTrace(System.err);
            }
        }else{
            tStatus.setText("Status: Not Connected");
        }
    }

    // Buttons Clicks
    public void buttonUpClick(View v){

        sendBluetoothMessage("1");
    }

    public void buttonRightClick(View v){

        sendBluetoothMessage("3");
    }

    public void buttonDownClick(View v){

        sendBluetoothMessage("2");
    }

    public void buttonLeftClick(View v){

        sendBluetoothMessage("4");
    }

    public void buttonStopClick(View v){

        sendBluetoothMessage("0");
    }

    public void buttonReversalClick(View v){

        sendBluetoothMessage("6");
    }

    public void buttonAutonomousClick(View v){

        sendBluetoothMessage("7");
    }

    public void buttonScan(MenuItem item){

        if(bSearch) return;
        BTArrayAdapter.clear();
        BTResultMac.clear();
        BTDevices.clear();
        mBluetoothAdapter.startDiscovery();
        registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        tStatus.setText("Status: Discovering...");
        bSearch = false;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d("!!!!!!!!!!!", "pd " + pairedDevices.size());
        //BTDevices.addAll(pairedDevices);

        for(BluetoothDevice device : pairedDevices){

            BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            BTArrayAdapter.notifyDataSetChanged();
            BTResultMac.add(device.getAddress());
            BTDevices.add(device);
        }

        scanTimer = new CountDownTimer(15000,1000){
            @Override
            public void onTick(long millisUntilFinished) {}
            @Override
            public void onFinish(){
                bSearch = true;
                mBluetoothAdapter.cancelDiscovery();
                tStatus.setText("Status: Search Finished");
            }
        };
        scanTimer.start();
    }

    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!BTDevices.contains(device)) {
                    BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    BTArrayAdapter.notifyDataSetChanged();
                    BTResultMac.add(device.getAddress());
                    BTDevices.add(device);
                }
            }
        }
    };

   /* @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                buttonScan(item);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }*/
}
