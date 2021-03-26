package ir.geraked.batteryban;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

public class PowerConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
//        Toast.makeText(context, "باتری بان", Toast.LENGTH_SHORT).show();
//        Intent i = new Intent();
//        i.setClassName("ir.geraked.batteryban", "ir.geraked.batteryban.MainActivity");
//        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);

        Intent serviceIntent = new Intent(context, ForegroundService.class);
        serviceIntent.putExtra("inputExtra", context.getText(R.string.app_running_detail));
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}
