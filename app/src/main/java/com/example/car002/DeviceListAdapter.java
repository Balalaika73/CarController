package com.example.car002;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    private LayoutInflater layoutInflater;      //экземляр макетов
    private int resourceView;           //идентификатор ресурса макета
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();

    public DeviceListAdapter(@NonNull Context context, int resource, ArrayList<BluetoothDevice> mdevices) {
        super(context, resource, mdevices);

        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        resourceView = resource;
        devices = mdevices;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {     //каждый элемент списка
        convertView = layoutInflater.inflate(resourceView, null);

        BluetoothDevice device = devices.get(position);

        TextView name = convertView.findViewById(R.id.nameDev);
        TextView address = convertView.findViewById(R.id.macDev);


        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return convertView;
        }
        name.setText(device.getName());
        address.setText(device.getAddress());

        return convertView;
    }
}
