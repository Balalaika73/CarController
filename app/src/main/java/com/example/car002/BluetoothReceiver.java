package com.example.car002;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageButton;

public class BluetoothReceiver extends BroadcastReceiver {
    private ImageButton turnOnB;

    public BluetoothReceiver(ImageButton turnOnB) {
        this.turnOnB = turnOnB;
    }

    @Override
    public void onReceive(Context context, Intent intent) { //изменение состояния bluetooth
        String action = intent.getAction();
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) { //сравнение действия
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);  //текущее состояние bluetooth
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    turnOnB.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24);
                    break;
                case BluetoothAdapter.STATE_OFF:
                    turnOnB.setImageResource(R.drawable.ic_baseline_bluetooth_24);
                    break;
            }
        }
    }
}

