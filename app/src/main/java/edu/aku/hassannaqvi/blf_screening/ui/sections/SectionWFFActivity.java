package edu.aku.hassannaqvi.blf_screening.ui.sections;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.validatorcrawler.aliazaz.Clear;
import com.validatorcrawler.aliazaz.Validator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.aku.hassannaqvi.blf_screening.R;
import edu.aku.hassannaqvi.blf_screening.contracts.FormsWFContract;
import edu.aku.hassannaqvi.blf_screening.core.DatabaseHelper;
import edu.aku.hassannaqvi.blf_screening.core.MainApp;
import edu.aku.hassannaqvi.blf_screening.databinding.ActivitySectionWffBinding;
import edu.aku.hassannaqvi.blf_screening.ui.other.MainActivity;
import edu.aku.hassannaqvi.blf_screening.workers.DataUpWorkerALL;

import static edu.aku.hassannaqvi.blf_screening.core.MainApp.formsWF;

public class SectionWFFActivity extends AppCompatActivity {

    ActivitySectionWffBinding bi;
    Intent oF = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bi = DataBindingUtil.setContentView(this, R.layout.activity_section_wff);
        bi.setCallback(this);
        setupSkips();
    }


    private void setupSkips() {
        bi.wf101.setOnCheckedChangeListener((group, i) -> Clear.clearAllFields(bi.fldGrpCVwfi02));
    }


    public void BtnContinue() {
        if (formValidation()) {
            try {
                SaveDraft();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (UpdateDB()) {
   /*             finish();
                startActivity(new Intent(this, MainActivity.class));*/
                bi.pBar3.setVisibility(View.VISIBLE);

                UploadData();

            }
        }
    }

    private boolean UploadData() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data data = new Data.Builder()
                .putString("table", "formsFUP")
                .putString("data", MainApp.formsWF.toJSONObject().toString())
                .build();

        //This is the subclass of our WorkRequest

        OneTimeWorkRequest dataUpload = new OneTimeWorkRequest.Builder(DataUpWorkerALL.class).setInputData(data).setConstraints(constraints).build();
        WorkManager.getInstance().enqueue(dataUpload);


        WorkManager.getInstance().getWorkInfoByIdLiveData(dataUpload.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(@Nullable WorkInfo workInfo) {


                        if (workInfo.getState() != null &&
                                workInfo.getState() == WorkInfo.State.SUCCEEDED) {

                            bi.wmError.setVisibility(View.GONE);
                            bi.pBar3.setVisibility(View.GONE);
                            //Displaying the status into TextView
                            //mTextView1.append("\n" + workInfo.getState().name());
                            String message = workInfo.getOutputData().getString("message");
                            DatabaseHelper db = new DatabaseHelper(SectionWFFActivity.this); // Database Helper
                            StringBuilder sSyncedError = new StringBuilder();
                            JSONObject jsonObject;
                            try {

                                JSONArray json = new JSONArray(message);
                                for (int i = 0; i < json.length(); i++) {
                                    jsonObject = new JSONObject(json.getString(i));

                                    if (!jsonObject.getString("error").equals("1")) {

                                        if (jsonObject.getString("status").equals("1")) {

                                            db.updateSyncedFormsEN(jsonObject.getString("id"));  // UPDATE SYNCED

                                            Toast.makeText(SectionWFFActivity.this, "Data saved successfully for: " + formsWF.getWfa101() + " - " + formsWF.getWfa101(), Toast.LENGTH_LONG).show();


                                            final Handler handler = new Handler(Looper.getMainLooper());
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //Do something after 100ms
                                                    /*oF = new Intent(SectionWFFActivity.this, MainActivity.class);
                                                    startActivity(oF);*/

                                                    Intent intent = new Intent();
                                                    intent.putExtra("mrno", formsWF.getWfa101());
                                                    setResult(1, intent);
                                                    finish();//finishing activity
                                                }
                                            }, 3500);

                                        } else {
                                            sSyncedError.append("\nError: ").append(jsonObject.getString("message"));
                                        }
                                    } else {
                                        bi.wmError.setText(jsonObject.getString("message"));
                                        bi.wmError.setVisibility(View.VISIBLE);
                                        bi.wmError.setTextColor(getResources().getColor(R.color.red));
                                        //bi.sf2.setError("MR No. already exists");
                                    }
                                }
                            } catch (JSONException e) {
                                bi.wmError.setText("JSON Error: " + message);
                                bi.wmError.setVisibility(View.VISIBLE);
                                Log.d("JSON Error", "onChanged: " + message);
                                e.printStackTrace();
                            }
                            //bi.sl2.setText(message);
                        }
                        //mTextView1.append("\n" + workInfo.getState().name());
                        if (workInfo.getState() != null &&
                                workInfo.getState() == WorkInfo.State.FAILED) {
                            bi.pBar3.setVisibility(View.GONE);
                            String message = workInfo.getOutputData().getString("error");
                            bi.wmError.setText(message);
                            bi.wmError.setVisibility(View.VISIBLE);

                        }
                    }
                });
        return false;
    }


    public void BtnEnd() {
        oF = new Intent(this, MainActivity.class);
        startActivity(oF);
    }


    private boolean UpdateDB() {
        DatabaseHelper db = MainApp.appInfo.getDbHelper();
        int updcount = db.updatesFormsWFColumn(FormsWFContract.FormsWFTable.COLUMN_SWFF, MainApp.formsWF.sWFFtoString());
        if (updcount == 1) {
            return true;
        } else {
            Toast.makeText(this, "Updating Database... ERROR!", Toast.LENGTH_SHORT).show();
            return false;
        }

    }


    private void SaveDraft() {

        MainApp.formsWF.setWf101(bi.wf10101.isChecked() ? "1"
                : bi.wf10102.isChecked() ? "2"
                : "-1");
        MainApp.formsWF.setWf10102x(bi.wf10102x.getText().toString().trim().isEmpty() ? "-1" : bi.wf10102x.getText().toString());

        MainApp.formsWF.setWfi02(bi.wfi02.getText().toString().trim().isEmpty() ? "-1" : bi.wfi02.getText().toString());

    }


    private boolean formValidation() {
        return Validator.emptyCheckingContainer(this, bi.GrpName);

    }


    @Override
    public void onBackPressed() {
        Toast.makeText(this, "You can't go back", Toast.LENGTH_SHORT).show();
    }


// START QR-CODE

    public void QRCode(View view) {

        // INTENT TO START QR-CODE CAMERA
        new IntentIntegrator(this).initiateScan(); // `this` is the current Activity
    }

    // Get the results:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {


                // TODO: SET CODE TO EDIT TEXT FIELD
                bi.wfi02.setText(result.getContents());


                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // END QR-CODE
}