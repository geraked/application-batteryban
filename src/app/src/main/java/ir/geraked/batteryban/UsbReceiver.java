package ir.geraked.batteryban;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class UsbReceiver extends BroadcastReceiver {

    public final String ACTION_USB_PERMISSION = "ir.geraked.batteryban.USB_PERMISSION";
    public UsbManager usbManager;
    public UsbDevice device;
    public UsbSerialDevice serialPort;
    public UsbDeviceConnection connection;
    public String data;
    public Context context;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            data = null;
            try {
                data = new String(arg0, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
            boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
            if (granted) {
                connection = usbManager.openDevice(device);
                serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                if (serialPort != null) {
                    if (serialPort.open()) {
                        // Set Serial Connection Parameters.
                        serialPort.setBaudRate(9600);
                        serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                        serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                        serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                        serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                        serialPort.read(mCallback);
                        setModuleStatus(true);
                    } else {
                        // Log.d("SERIAL", "PORT NOT OPEN");
                    }
                } else {
                    // Log.d("SERIAL", "PORT IS NULL");
                }
            } else {
                // Log.d("SERIAL", "PERM NOT GRANTED");
            }
        } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            openConnection();
        } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
            closeConnection();
        }
    }

    public void openConnection() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 1659) {
                    PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
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

    public void closeConnection() {
        serialPort.close();
        setModuleStatus(false);
    }

    public boolean sendSignal(String str) {
        if (serialPort == null) {
            setModuleStatus(false);
            openConnection();
            return false;
        }
        serialPort.write(str.getBytes());
        return true;
    }

    public void sendOnSignal() {
        if (sendSignal("1")) {
            // msg(getResources().getString(R.string.sent_on_signal));
        }
    }

    public void sendOffSignal() {
        if (sendSignal("0")) {
            // msg(getResources().getString(R.string.sent_off_signal));
        }
    }

    public void setModuleStatus(boolean status) {
    }
}
