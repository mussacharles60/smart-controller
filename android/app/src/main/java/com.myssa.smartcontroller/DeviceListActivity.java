package com.myssa.smartcontroller;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.myssa.smartcontroller.databinding.ActivityDeviceListBinding;
import com.myssa.smartcontroller.databinding.DeviceListItemViewBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeviceListActivity extends AppCompatActivity {

    public static final String TAG = "DeviceListActivity";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_DEVICE_NAME = "device_name";

    public static final int GET_DEVICE_REQUEST_CODE = 2022;
    private ActivityDeviceListBinding binding;

    private DeviceListAdepter adapter;
    private BluetoothHelper.Scanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "onCreate: ");

        binding.infoText.setVisibility(View.GONE);
        binding.infoText2.setVisibility(View.GONE);
        binding.emptyText.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerView.setVisibility(View.GONE);

        binding.settingsBtn.setOnClickListener(v -> {
            // open bluetooth settings
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        });

        if (!BluetoothHelper.isBluetoothEnabled()) {
            Log.d(TAG, "onCreate: bluetooth not enabled");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        adapter = new DeviceListAdepter();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        binding.recyclerView.setNestedScrollingEnabled(false);
        ((SimpleItemAnimator) Objects.requireNonNull(binding.recyclerView.getItemAnimator())).setSupportsChangeAnimations(true);
        binding.recyclerView.setAdapter(adapter);

        scanner = new BluetoothHelper.Scanner(this,
                new BluetoothHelper.ScannerListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onAdapterEnabled() {
                        Log.d(TAG, "onAdapterEnabled: scanning...");
                        binding.progressBar.setVisibility(View.VISIBLE);
                        binding.infoText.setVisibility(View.VISIBLE);
                        binding.infoText.setText("Scanning...");
                        binding.infoText2.setVisibility(View.GONE);
                        new android.os.Handler().postDelayed(() -> {
                            if (isFinishing()) {
                                return;
                            }
                            if (scanner != null) {
                                scanner.scanDevices();
                            }
                        }, 1000);
                    }

                    @Override
                    public void onAdapterDisabled() {
                        Log.d(TAG, "onAdapterDisabled: ");
                        setResult(RESULT_CANCELED);
                        finish();
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDevicesFound(List<BluetoothHelper.Scanner.Device> devices) {
                        Log.d(TAG, "onDevicesFound: list: size: " + devices.size());
                        binding.progressBar.setVisibility(View.GONE);
                        binding.infoText2.setVisibility(View.VISIBLE);

                        if (devices.isEmpty()) {
                            binding.infoText.setVisibility(View.GONE);
                            binding.emptyText.setVisibility(View.VISIBLE);
                            binding.emptyText.setText("No devices found");
                            binding.recyclerView.setVisibility(View.GONE);
                        } else {
                            binding.infoText.setVisibility(View.VISIBLE);
                            binding.infoText.setText("Select a device to connect");
                            binding.emptyText.setVisibility(View.GONE);
                            binding.recyclerView.setVisibility(View.VISIBLE);
                            binding.recyclerView.post(() -> adapter.setList(devices));
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanner != null) {
            scanner.stop();
            scanner = null;
        }
    }

    //    public interface OnDeviceSelectedListener {
    //        void onDeviceSelected(@NonNull BluetoothHelper.Scanner.Device device);
    //    }

    private class DeviceListAdepter extends RecyclerView.Adapter<DeviceListAdepter.DeviceViewHolder> {

        private final List<BluetoothHelper.Scanner.Device> devices = new ArrayList<>();

        public DeviceListAdepter() {
            super();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void setList(List<BluetoothHelper.Scanner.Device> devices) {
            this.devices.clear();
            this.devices.addAll(devices);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DeviceViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.device_list_item_view, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            holder.bind(devices.get(position));
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        private class DeviceViewHolder extends RecyclerView.ViewHolder {

            private final DeviceListItemViewBinding binding;

            public DeviceViewHolder(@NonNull View itemView) {
                super(itemView);
                binding = DeviceListItemViewBinding.bind(itemView);
            }

            public void bind(@NonNull BluetoothHelper.Scanner.Device device) {
                binding.name.setText(device.getName());
                binding.address.setText(device.getAddress());

                binding.getRoot().setOnClickListener(v -> {
                    Intent intent = getIntent();
                    intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
                    intent.putExtra(EXTRA_DEVICE_NAME, device.getName());
                    setResult(RESULT_OK, intent);
                    finish();
                });
            }
        }
    }
}
