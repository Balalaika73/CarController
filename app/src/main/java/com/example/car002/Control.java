package com.example.car002;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class Control extends AppCompatActivity {

    Button forward, back, left;
    ImageButton right;
    String MAC = null;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private BluetoothDevice blD;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        forward = findViewById(R.id.forward);
        back = findViewById(R.id.back);
        left = findViewById(R.id.left);
        right = findViewById(R.id.right);

        blD = getIntent().getParcelableExtra("device"); //получение данных подключенного устройства
        MAC = blD.getAddress();

        startConnection(blD);

        right.setOnTouchListener((v, event) -> {
            sendCommand("R", event);
            return true;
        });
        forward.setOnTouchListener((v, event) -> {
            sendCommand("F", event);
            return true;
        });
        left.setOnTouchListener((v, event) -> {
            sendCommand("L", event);
            return true;
        });
        back.setOnTouchListener((v, event) -> {
            sendCommand("B", event);
            return true;
        });
    }

    private void sendCommand(String command, MotionEvent event) {       //отправка данных
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                try {
                    outputStream.write(command.getBytes());     //пребразование в байтовый массив
                    outputStream.flush();
                    Log.i("WTF", command + " отправлено");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("WTF", "Команда " + command+" не отправлена");
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                try {
                    outputStream.write("S".getBytes());
                    outputStream.flush();
                    Log.i("WTF", "S отправлено");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("WTF", "Не удалось отправить S");
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {     //меню управления активностью
        getMenuInflater().inflate(R.menu.control_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

            @Override       //действие при выборе элемента меню
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.deviceout:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private String getUUID(String addres) {     //получает UUID для создания связи
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = adapter.getRemoteDevice(MAC);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        device.fetchUuidsWithSdp();

        ParcelUuid[] uuids = device.getUuids();         //сохранение полученных UUID
        if (uuids != null) {
            for (ParcelUuid uuid : uuids) {
                Log.d("UUID", uuid.toString());
                return uuid.toString();
            }
        }
        return null;
    }

    private void startConnection(BluetoothDevice device) {
        if (device != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(getUUID(device.getAddress())));
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();       //запись данных на сокет (отправка по bluetooth)
            } catch (Exception e) {
                Log.i("WTF", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {        //закрытие фонофых gотоков
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}