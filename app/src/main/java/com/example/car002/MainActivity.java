package com.example.car002;

import static android.content.ContentValues.TAG;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final int REQ_ENABLE_BLUETOOTH = 1001;
    TextView textView;
    boolean isConnected;
    Button toControl;
    ImageButton turnOnB, turnOnG;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ListView listDev;
    ProgressBar pb;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private DeviceListAdapter deviceListAdapter;
    BluetoothAdapter bluetoothAdapter;
    private LocationManager locationManager;
    private BluetoothReceiver bluetoothReceiver;
    String MAC = null;
    private BluetoothDevice blD;
    private String targetDevice = null;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pb = findViewById(R.id.pb);
        pb.setVisibility(View.INVISIBLE);

        textView = findViewById(R.id.textView);
        toControl = findViewById(R.id.toControl);
        turnOnB = (ImageButton) findViewById(R.id.blueturn);
        turnOnG = (ImageButton) findViewById(R.id.gpsturn);
        isConnected = false;

        toControl.setBackgroundColor(0);
        toControl.setBackgroundColor(getResources().getColor(R.color.button_background));
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Проверка состояния GPS и установка соответствующей иконки
        if (locationManager.isLocationEnabled()) {
            turnOnG.setImageResource(R.drawable.ic_baseline_location_off_24);
        } else {
            turnOnG.setImageResource(R.drawable.ic_baseline_location_on_24);
        }
        //Debug.waitForDebugger();

        // Проверка и запрос разрешения на доступ к местоположению
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }

        // Проверка поддержки Bluetooth и инициализация адаптера
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "BLUTOOTH не поддерживается", Toast.LENGTH_SHORT).show();
            return;
        }

        // Инициализация приёмника событий Bluetooth и адаптера списка устройств
        bluetoothReceiver = new BluetoothReceiver(turnOnB);
        deviceListAdapter = new DeviceListAdapter(this, R.layout.device_item, devices);

        turnOnB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothON();
                if (bluetoothAdapter.isEnabled())
                    turnOnB.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24);
                else
                    turnOnB.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24);
            }
        });

        turnOnG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableLocation();
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    turnOnG.setImageResource(R.drawable.ic_baseline_location_off_24);
                } else {
                    turnOnG.setImageResource(R.drawable.ic_baseline_location_on_24);
                }
            }
        });

        toControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected == true) {
                    try {
                        bluetoothSocket.close();
                        Intent intent = new Intent(MainActivity.this, Control.class);
                        intent.putExtra("device", blD);
                        startActivity(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBluetoothStatus();        //отслеживание Bluetooth телефона
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            turnOnG.setImageResource(R.drawable.ic_baseline_location_off_24);
        } else {
            turnOnG.setImageResource(R.drawable.ic_baseline_location_on_24);
        }
    }

    @Override
    protected void onPause() {      //приостановка активности
        super.onPause();
        unregisterReceiver(bluetoothReceiver);
    }

    @Override
    protected void onDestroy() {        //уничтожение активности
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {     //создание меню
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {      //действие меню
        switch (item.getItemId()) {
            case R.id.findevices:
                searchDevices();
                break;
            case R.id.deviceout:
                finish();
                break;
            case R.id.findevice:
                showDeviceSearchDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeviceSearchDialog() {         //окно поиска определенного устройства
        AlertDialog.Builder builder = new AlertDialog.Builder(this);        //создание диалогового окна
        builder.setTitle("Поиск устройства");

        final EditText deviceNameEditText = new EditText(this);
        deviceNameEditText.setInputType(InputType.TYPE_CLASS_TEXT);     //тип ввода для поля
        builder.setView(deviceNameEditText);

        builder.setPositiveButton("Поиск", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String deviceName = deviceNameEditText.getText().toString().trim();
                targetDevice = deviceName;
                if (!deviceName.isEmpty()) {
                    searchDeviceByName(deviceName);
                }
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.create().show();
    }

    private void searchDeviceByName(String deviceName) {        //поиск устройства
        devices.clear();
        deviceListAdapter.clear();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int check = checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            check += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (check != 0) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1002);
            }
        }

        if (!bluetoothAdapter.isDiscovering()) {
            Log.i("TAG", "searchDevices: начинаем поиск устройств.");
            bluetoothAdapter.startDiscovery();
        }

        if (bluetoothAdapter.isDiscovering()) {
            Log.i("TAG", "searchDevices: поиск уже был запущен... перезапускаем его еще раз.");
            bluetoothAdapter.cancelDiscovery();
            bluetoothAdapter.startDiscovery();
        }
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver1, intentFilter);
    }


    private void showMessage(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }       //метод сообщений

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {       //получение результат другой активности bluetooth
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ENABLE_BLUETOOTH) {
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Повторно пытаемся включить bluetooth");
                BluetoothON();
            }
        }
    }

    private void startConnection(BluetoothDevice device) throws NoSuchMethodException {     //установление соединения
        if (device != null) {
            try {
                Method method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                bluetoothSocket = (BluetoothSocket) method.invoke(device, 1);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;
                showMessage("Подключение успешно");
                textView.setText(device.getName());
                MAC = device.getAddress();
                blD = device;
            } catch (Exception e) {
                showMessage("Ошибка подключения");
                e.printStackTrace();
            }
        }
    }

    private void searchDevices() {          //общий поиск устройств
        devices.clear();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int check = checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            check += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (check != 0) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1002);
            }
        }

        if (!bluetoothAdapter.isDiscovering()) {
            Log.i("TAG", "searchDevices: начинаем поиск устройств.");
            bluetoothAdapter.startDiscovery();
        }

        if (bluetoothAdapter.isDiscovering()) {
            Log.i("TAG", "searchDevices: поиск уже был запущен... перезапускаем его еще раз.");
            bluetoothAdapter.cancelDiscovery();
            bluetoothAdapter.startDiscovery();
        }
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void showListDevices() {            //результат поиска устройств
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Устройства");

        View view = getLayoutInflater().inflate(R.layout.device_list, null);
        listDev = view.findViewById(R.id.list_dev);
        listDev.setAdapter(deviceListAdapter);
        listDev.setOnItemClickListener(itemClickListener);
        builder.setView(view);
        builder.setNegativeButton("OK", null);
        builder.create();
        builder.show();
    }

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {     //событие выбора устройства
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = devices.get(position);
            Log.i("Device", device.toString());
            try {
                startConnection(device);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    };

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                Log.d(TAG, "ACTION_DISCOVERY_STARTED");
                showMessage("Начат поиск устройств");
                pb = findViewById(R.id.pb);
                pb.setVisibility(View.VISIBLE);
                deviceListAdapter.clear();
            }
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d(TAG, "ACTION_DISCOVERY_FINISHED");
                showMessage("Поиск устройств завершен");
                pb.setVisibility(View.INVISIBLE);
                showListDevices();
            }
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                Log.d(TAG, "ACTION_FOUND");
                showMessage("Начат поиск устройств");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (!devices.contains(device)) {
                        Log.d("TAG", device.getAddress());
                        deviceListAdapter.add(device);
                    }
                }
            }
        }
    };

    BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver() {
        List<BluetoothDevice> foundDevices = new ArrayList<>();

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                Log.d(TAG, "ACTION_DISCOVERY_STARTED");
                showMessage("Начат поиск устройства");
                pb = findViewById(R.id.pb);
                pb.setVisibility(View.VISIBLE);
                foundDevices.clear();
                deviceListAdapter.clear();
            }
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d(TAG, "ACTION_DISCOVERY_FINISHED");
                showMessage("Поиск устройств завершен");
                pb.setVisibility(View.INVISIBLE);
                // Add only the matched device to the adapter
                for (BluetoothDevice device : foundDevices) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if (device.getName() != null && device.getName().equals(targetDevice)) {
                        deviceListAdapter.add(device);
                        bluetoothAdapter.cancelDiscovery();
                    }
                }
                showListDevices();
            }
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                Log.d(TAG, "ACTION_FOUND");
                showMessage("Начат поиск устройства");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    foundDevices.add(device);
                }
            }
        }
    };


    public void enableLocation() {      //проверка состояния местоположения
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Служба местоположения уже включена", Toast.LENGTH_SHORT).show();
        }
    }


    public boolean BluetoothON() {      //проверка состояния Bluetooth
        if (!bluetoothAdapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
            startActivity(turnOn);
        } else {
            Toast.makeText(this, "Служба Bluetooth уже включена", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {     //разрешение доступа к местоположению
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation();
            } else {
                Toast.makeText(this, "Доступ к локации запрещен", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateBluetoothStatus() {
        if (bluetoothAdapter.isEnabled()) {
            // Bluetooth включен
            turnOnB.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24);
        } else {
            // Bluetooth выключен
            turnOnB.setImageResource(R.drawable.ic_baseline_bluetooth_24);
        }
    }

}