package ir.geraked.batteryban;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.john.waveview.WaveView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    TextView batteryLevelTxt;
    TextView tv;
    TextView tempVolTxt;
    TextView goalTxt;
    WaveView wave;
    SeekBar seekBar;
    TextView moduleStatusTxt;
    ImageView moduleStatusImg;
    TextView chargerStatusTxt;
    ImageView chargerStatusImg;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    BatteryReceiver batteryReceiver;
    UsbReceiver usbReceiver;

    int goalPercent;

    private int _goalPercent;
    private TimerTask goalPercentTimerTask;
    private Timer goalPercentTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        batteryLevelTxt = findViewById(R.id.batteryLevelTxt);
        tempVolTxt = findViewById(R.id.tempVol);
        goalTxt = findViewById(R.id.goalTxt);
        wave = findViewById(R.id.wave);
        seekBar = findViewById(R.id.seekBar);
        moduleStatusTxt = findViewById(R.id.moduleTxt);
        moduleStatusImg = findViewById(R.id.moduleImg);
        chargerStatusTxt = findViewById(R.id.chargerTxt);
        chargerStatusImg = findViewById(R.id.chargerImg);
        tv = findViewById(R.id.textView);

        sharedPref = getSharedPreferences("ir.geraked.batteryban.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        set_goalPercent(sharedPref.getInt("GOAL_PERCENT", 80));
        setModuleStatusView(false);
        setChargerStatusView(false);

        initiateBatteryReceiver();
        initiateUsbReceiver();
        setSeekBarListener();

        startService();
    }

    public void startService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.putExtra("inputExtra", getText(R.string.app_running_detail));
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }

    void initiateBatteryReceiver() {
        batteryReceiver = new BatteryReceiver() {
            @Override
            public void onReceiveDo() {
                onBatteryReceive();
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    void initiateUsbReceiver() {
        usbReceiver = new UsbReceiver() {
            @Override
            public void setModuleStatus(boolean status) {
                setModuleStatusView(status);
            }
        };
        usbReceiver.context = this;
        usbReceiver.usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(usbReceiver.ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
    }

    void setSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                set_goalPercent(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    void msg(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    public void sendOn(View v) {
        usbReceiver.sendOnSignal();
    }

    public void sendOff(View v) {
        usbReceiver.sendOffSignal();
    }

    void onBatteryReceive() {
        setBatteryLevel();
        setChargerStatusView(batteryReceiver.isCharging);
        tempVolTxt.setText(batteryReceiver.temperature + Character.toString((char) 176) + "C / " + batteryReceiver.voltage + "V");
    }

    void setBatteryLevel() {
        int batteryPercent = batteryReceiver.batteryPercent;
        boolean isCharging = batteryReceiver.isCharging;

        batteryLevelTxt.setText(batteryPercent + "");
        if (batteryPercent >= goalPercent && isCharging) {
            usbReceiver.sendOffSignal();
        } else if (batteryPercent <= goalPercent - 5 && !isCharging) {
            usbReceiver.sendOnSignal();
        }

        int wavePercent = batteryPercent;
        if (batteryPercent > 95) {
            wavePercent = 95;
        } else if (batteryPercent < 5) {
            wavePercent = 5;
        } else if (batteryPercent < 50) {
            wavePercent = batteryPercent + 100 / batteryPercent;
        } else if (batteryPercent > 50) {
            wavePercent = (int) (batteryPercent - 0.03 * batteryPercent);
        }
        wave.setProgress(wavePercent);

        if (batteryPercent < 10) {
            wave.setBackgroundColor(getResources().getColor(R.color.colorVeryLow));
        } else if (batteryPercent < 15) {
            wave.setBackgroundColor(getResources().getColor(R.color.colorLow));
        } else {
            wave.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        }
    }

    void startGoalPercentTimer() {
        if (goalPercentTimer != null) {
            goalPercentTimer.cancel();
            goalPercentTimer.purge();
        }
        goalPercentTimer = new Timer();
        goalPercentTimerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        goalPercent = _goalPercent;
                        editor.putInt("GOAL_PERCENT", goalPercent);
                        editor.apply();
                        // msg(getResources().getString(R.string.goal_saved));
                    }
                });
            }
        };
        goalPercentTimer.schedule(goalPercentTimerTask, 2000);
    }

    private void set_goalPercent(int x) {
        if (x >= 100) {
            _goalPercent = 100;
        } else if (x <= 0) {
            _goalPercent = 0;
        } else {
            _goalPercent = x;
        }
        goalTxt.setText(getResources().getString(R.string.goal) + " " + _goalPercent);
        seekBar.setProgress(_goalPercent);
        startGoalPercentTimer();
    }

    public void onClickIncrease(View v) {
        set_goalPercent(_goalPercent + 1);
    }

    public void onClickDecrease(View v) {
        set_goalPercent(_goalPercent - 1);
    }

    public void setModuleStatusView(boolean b) {
        if (b) {
            moduleStatusImg.setColorFilter(getResources().getColor(R.color.colorInfo));
            moduleStatusTxt.setText(getResources().getString(R.string.module_connected));
        } else {
            moduleStatusImg.setColorFilter(getResources().getColor(R.color.colorWarning));
            moduleStatusTxt.setText(getResources().getString(R.string.module_not_connected));
        }
    }

    public void setChargerStatusView(boolean b) {
        if (b) {
            chargerStatusImg.setColorFilter(getResources().getColor(R.color.colorInfo));
            chargerStatusTxt.setText(getResources().getString(R.string.charging));
        } else {
            chargerStatusImg.setColorFilter(getResources().getColor(R.color.colorWarning));
            chargerStatusTxt.setText(getResources().getString(R.string.not_charging));
        }
    }
}
