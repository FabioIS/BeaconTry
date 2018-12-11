package com.example.beacontry1;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity implements BeaconConsumer, RangeNotifier {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    protected static final String TAG = "MainActivity";
    private BeaconManager mBeaconManager;
    private Map<String, String> blueUp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        blueUp = new HashMap<>();
        blueUp.put("0xacfd065e1a514932ac01", "BlueUp1");

        mBeaconManager = BeaconManager.getInstanceForApplication(this);

        //Le decimos al beaconManager que protocolo vamos a utilizar.
        mBeaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));

        mBeaconManager.setForegroundBetweenScanPeriod(2000L);
       // mBeaconManager.setForegroundScanPeriod(1000L);
        mBeaconManager.bind(this);

        if ( SDK_INT   >= M ) {
            // Android M Permission check
            if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {


                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {


        ArrayList<Identifier> identifiers = new ArrayList<>();
        //identifiers.add(null);

        Region region = new Region("AllBeaconsRegion", identifiers);
        Log.e("TAG", "Estoy Aquí tambien");
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mBeaconManager.addRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        Log.e("TAG", "Estoy Aquí");
        Log.e("TAG", "Longitud = " + beacons.size());

        if (beacons.size() > 0) {
            for(int i = 0; i < beacons.size(); i++) {
                Beacon beacon = beacons.iterator().next();
                if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                    if (blueUp.containsKey(beacon.getId1().toString())) {
                        showToastMessage("El beacon numero " + blueUp.get(beacon.getId1().toString()) + " se encuentra a una distancia de "
                                + beacon.getDistance() + " metros");
                    }
                    if (beacon.getExtraDataFields().size() > 0) {
                        long telemetryVersion = beacon.getExtraDataFields().get(0);
                        long batteryMilliVolts = beacon.getExtraDataFields().get(1);
                        long pduCount = beacon.getExtraDataFields().get(3);
                        long uptime = beacon.getExtraDataFields().get(4);

                        Log.d(TAG, "The above beacon is sending telemetry version "+telemetryVersion+
                                ", has been up for : "+uptime+" seconds"+
                                ", has a battery level of "+batteryMilliVolts+" mV"+
                                ", and has transmitted "+pduCount+" advertisements.");

                    }
                }
            }
        }
    }

    private void showToastMessage(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
