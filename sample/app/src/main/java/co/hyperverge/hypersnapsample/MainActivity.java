package co.hyperverge.hypersnapsample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import co.hyperverge.hypersnapsdk.R;
import co.hyperverge.hypersnapsdk.activities.HVDocsActivity;
import co.hyperverge.hypersnapsdk.activities.HVFaceActivity;
import co.hyperverge.hypersnapsdk.listeners.APICompletionCallback;
import co.hyperverge.hypersnapsdk.listeners.CaptureCompletionHandler;
 import co.hyperverge.hypersnapsdk.network.HVNetworkHelper;
import co.hyperverge.hypersnapsdk.objects.Error;
import co.hyperverge.hypersnapsdk.objects.HVDocConfig;
import co.hyperverge.hypersnapsdk.objects.HVFaceConfig;



public class MainActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private final int MY_PERMISSIONS_REQUEST_CAMERA_ACTIVITY = 101;
    private ArrayList<String> runtimePermissions = new ArrayList<>(Arrays.asList(Manifest.permission.CAMERA));

    /**
     * Document and Liveness have been moved to their respective parent configuration classes.
     */
    private HVDocConfig.Document selectedDocument;
    HVFaceConfig.LivenessMode mode = HVFaceConfig.LivenessMode.TEXTURELIVENESS;
    TextView resultView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        resultView = (TextView) findViewById(R.id.tv_result);
        findViewById(R.id.tv_a4).setOnClickListener(this);
        findViewById(R.id.tv_other).setOnClickListener(this);
        findViewById(R.id.tv_card).setOnClickListener(this);
        findViewById(R.id.tv_passport).setOnClickListener(this);
        findViewById(R.id.tv_face).setOnClickListener(this);
        ((RadioGroup) (findViewById(R.id.face_value))).setOnCheckedChangeListener(this);
        PermissionManager manager = new PermissionManager();
        manager.checkAndRequestPermissions(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA_ACTIVITY:

                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(MainActivity.this, "Cannot start as all the permissions were not granted", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                return;
        }
    }

    private void getPermissions() {
        ArrayList<String> missingPermissions = checkForMissingPermissions();
        // Assume thisActivity is the current activity

        if (missingPermissions.size() == 0) {
            return;
        }

        ArrayList<String> toBeRequestedPermissions = new ArrayList<>();
        ArrayList<String> rationalePermissions = new ArrayList<>();
        for (String missingPermission : missingPermissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    missingPermission)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                rationalePermissions.add(missingPermission);

            } else {

                // No explanation needed, we can request the permission.
                toBeRequestedPermissions.add(missingPermission);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (toBeRequestedPermissions.size() > 0) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    toBeRequestedPermissions.toArray(new String[0]),
                    MY_PERMISSIONS_REQUEST_CAMERA_ACTIVITY);
        }
        if (rationalePermissions.size() > 0) {
            String permissionsTxt = "";
            for (String perm : rationalePermissions) {
                String[] permSplit = perm.split("\\.");
                permissionsTxt += permSplit[permSplit.length - 1] + ", ";
            }

            permissionsTxt = permissionsTxt.substring(0, permissionsTxt.length() - 2);
            Toast.makeText(MainActivity.this, "Please give " + permissionsTxt + " permissions by going to Settings", Toast.LENGTH_LONG).show();
        }
    }

    public ArrayList<String> checkForMissingPermissions() {
        ArrayList<String> missingPermissions = new ArrayList<>();
        for (String permission : runtimePermissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }


    public void startAppropriateDocumentActivity(final HVDocConfig docConfig) {

        HVDocsActivity.start(MainActivity.this, docConfig, new CaptureCompletionHandler() {
            @Override
            public void onResult(Error error, JSONObject result) {

                if (error != null) {
                    resultView.setText("ERROR: " + error.getError().toString() + " Msg: " + error.getErrMsg());
                } else {
                    resultView.setText("RESULT: " + result.toString());
                    try {
                        HVNetworkHelper.makeOCRCall("https://ind.docs.hyperverge.co/v1-1/readPassport", result.getString("imageUri"), new JSONObject(), new APICompletionCallback() {
                            @Override
                            public void onResult(JSONObject error, JSONObject result) {
                                if(error!= null)
                                    resultView.setText("RESULT: " + error.toString());
                                if(result != null) {
                                    resultView.setText("RESULT: " + result.toString());
                                }
                            }
                        });
                        Glide.with(MainActivity.this).load(result.getString("imageUri")).into((ImageView) findViewById(R.id.iv_result));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                ScrollView sv = findViewById(R.id.sv_main);
                sv.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setLocale();

    }

    public void setLocale() {
        /**
         * To set a locale for country, the contry code is set in Locale constructor
         * Locale locale = new Locale("vi") - Vietnam
         */
        Locale locale = new Locale("");
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        Resources res = getBaseContext().getResources();
        res.updateConfiguration(config, res.getDisplayMetrics());

    }

    public void startFaceCaptureActivity() {
        /**
         * HVFaceConfig is the configuration class to set parameters for HVFaceActivity
         */
        HVFaceConfig config = new HVFaceConfig();
        // config.setLivenessMode(mode);
        config.setFaceCaptureTitle(" Face capture  ");
        config.setShouldShowInstructionPage(true);
        HVFaceActivity.start(MainActivity.this, config, new CaptureCompletionHandler() {
            @Override
            public void onResult(Error error, final JSONObject result) {
                if (error != null) {
                    resultView.setText(" Msg: " + error.getErrMsg() + "ERROR: " + error.getError().name());
                } else {
                    try {
                        resultView.setText(result.toString());
                        Toast.makeText(MainActivity.this, result.getString("imageUri"), Toast.LENGTH_SHORT).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Glide.with(MainActivity.this).load(result.getString("imageUri")).dontAnimate().into((ImageView) findViewById(R.id.iv_result));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 100);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    public void onClick(View view) {
        /**
         * HVDocConfig is the configuration class to set parameters for HVDocsActivity
         */

        HVDocConfig docConfig = new HVDocConfig();
        docConfig.setShouldShowFlashIcon(false);
        docConfig.setDocReviewDescription("Is your document fully visible, glare free and not blurred ?");
        docConfig.setDocReviewTitle("Review your photo");
        docConfig.setDocCaptureTitle("Docs Capture");
        docConfig.setDocCaptureDescription("Make sure your document is without any glare and is fully inside");
        docConfig.setDocCaptureSubText("Front side");
        docConfig.setShouldShowReviewScreen(true);
        docConfig.setShouldShowInstructionPage(true);
        if (view.getId() == R.id.tv_a4) {
            selectedDocument = HVDocConfig.Document.A4;

            docConfig.setDocumentType(selectedDocument);
            startAppropriateDocumentActivity(docConfig);
        }
        if (view.getId() == R.id.tv_card) {
            selectedDocument = HVDocConfig.Document.CARD;
            docConfig.setDocumentType(selectedDocument);
            startAppropriateDocumentActivity(docConfig);
        }
        if (view.getId() == R.id.tv_other) {
            selectedDocument = HVDocConfig.Document.OTHER;
            docConfig.setDocumentType(selectedDocument);
            startAppropriateDocumentActivity(docConfig);
        }
        if (view.getId() == R.id.tv_passport) {
            selectedDocument = HVDocConfig.Document.PASSPORT;
            docConfig.setDocumentType(selectedDocument);
            startAppropriateDocumentActivity(docConfig);
        }

        if (view.getId() == R.id.tv_face) {
            startFaceCaptureActivity();
        }
    }



    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int id) {
        if (id == R.id.texture_liveness) {
            mode = HVFaceConfig.LivenessMode.TEXTURELIVENESS;
        } else if (id == R.id.gesture_liveness) {
            mode = HVFaceConfig.LivenessMode.TEXTUREANDGESTURELIVENESS;
        } else if (id == R.id.none_liveness) {
            mode = HVFaceConfig.LivenessMode.NONE;
        }
    }
}