package parmar.kapil.nearyou;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;


import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

/**
 * Created by kapil on 1/8/2018.
 */

public class SplashActivity extends Activity {
    private static final int PERMISSION_ALL = 0;
    private Handler handler;
    private Runnable runnable;

    /*
      SharedPreferences mPrefs;
      final String settingScreenShownPref = "settingScreenShown";
      final String versionCheckedPref = "versionChecked";
    */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        try {


            handler = new Handler() {};
            runnable = new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, ActivityMain.class);
                    startActivity(intent);
                    finish();
                }
            };
            String[] PERMISSIONS = {
                    ACCESS_FINE_LOCATION,
                    ACCESS_NETWORK_STATE,

            };
            if (!hasPermissions( PERMISSIONS)) {
                Toast.makeText(this, "Location, storage and device permissions are a must", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            } else
                handler.postDelayed(runnable, 1500);
        }catch (Exception ex) {
            Toast.makeText(this, "Location, storage and device permissions are a must", Toast.LENGTH_SHORT).show();
        }
    }
    public  boolean hasPermissions( String... allPermissionNeeded)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && allPermissionNeeded != null)
            for (String permission : allPermissionNeeded)
                if (checkSelfPermission( permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
        return true;
    }
    // Put the below OnRequestPermissionsResult code here
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {


        int index = 0;
        Map<String, Integer> PermissionsMap = new HashMap<String, Integer>();
        for (String permission : permissions){
            PermissionsMap.put(permission, grantResults[index]);
            index++;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M  && (checkSelfPermission
                (Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission
                (Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission
                (Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED )){
            Toast.makeText(this, "Location, storage and device permissions are required.", Toast.LENGTH_SHORT).show();
            finish();
        }
        else
        {
            handler.postDelayed(runnable, 500);
        }
    }
}