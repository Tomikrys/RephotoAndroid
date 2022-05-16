package com.vyw.rephotoandroid;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    static {
        System.loadLibrary("native-lib");
    }


    Dialog dialog;
    int simple_navigation_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_layout);
        simple_navigation_button = findViewById(R.id.simple_navigation).getId();

//  TODO debug memory mode
//        if(BuildConfig.DEBUG)
//            StrictMode.enableDefaults();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void playApplication(View view) {
        // leaked vindow
        setContentView(R.layout.config_layout);
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.config_layout);
        dialog.setTitle("Error message");
        dialog.show();
    }

    public void endApplication(View view) {
        System.exit(0);
    }

//    public void simpleNavigation(View view, Bitmap ref_image) {
//        Intent intent = new Intent(this, SimpleNavigation.class);
//        intent.putExtra("REF_IMAGE", ref_image);
//        intent.putExtra("PATH_REF_IMAGE", "");
//        startActivity(intent);
//    }

    public void simpleNavigation(View view) {
//        ActivityCompat.finishAffinity(this);
        showFileDialog(view);
        Intent intent = new Intent(this, SimpleNavigation.class);
        intent.putExtra("PATH_REF_IMAGE", path_ref_image);
        intent.putExtra("SOURCE", "");
        startActivity(intent);
//        finish();
    }

    private static final int REQUEST_CHOOSER = 1234;
    int id_entered_button;
    String path_ref_image = "";

    public void showFileDialog(View view) {
        id_entered_button = view.getId();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent = Intent.createChooser(intent, "Choose file");

        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final Uri uri = data.getData();
            String path = uri.toString();

            if (path != null) {
                if (Integer.compare(id_entered_button, simple_navigation_button) == 0) {
                    path_ref_image = path;
                    TextView textView = (TextView) findViewById(R.id.ref_frame_label);
                    textView.setText(path_ref_image);
                }

            }
            Log.d(TAG, path);
            Log.d(TAG, String.valueOf(id_entered_button));
        }
    }

    public void insertCalibrationData(View view) {
        String regexStr = "^[0-9]*$";
        EditText tf[] = new EditText[8];
        String params[] = new String[8];
        String message = "";
        boolean isCorrect = true;

        tf[0] = (EditText) dialog.findViewById(R.id.tf_cxf);
        tf[1] = (EditText) dialog.findViewById(R.id.tf_cyf);
        tf[2] = (EditText) dialog.findViewById(R.id.tf_fxf);
        tf[3] = (EditText) dialog.findViewById(R.id.tf_fyf);
        tf[4] = (EditText) dialog.findViewById(R.id.tf_cxc);
        tf[5] = (EditText) dialog.findViewById(R.id.tf_cyc);
        tf[6] = (EditText) dialog.findViewById(R.id.tf_fxc);
        tf[7] = (EditText) dialog.findViewById(R.id.tf_fyc);

        for(int i = 0; i < tf.length; i++){
            params[i] = tf[i].getText().toString();
            message += params[i] + ";";
            if (!params[i].trim().matches(regexStr)) {
                TextView error_message = (TextView) dialog.findViewById(R.id.error_message);
                error_message.setVisibility(View.VISIBLE);
                isCorrect = false;
                break;
            }
        }

        if(isCorrect){
//            ActivityCompat.finishAffinity(this);
            dialog.dismiss();
            Intent intent = new Intent(this, SettingActivity.class);
            intent.putExtra(EXTRA_MESSAGE, message);
            startActivity(intent);
//            finish();
        }
    }

    public void exitApplication(MenuItem item) {
        finish();
    }
}
