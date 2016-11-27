package com.nomasp.expression.other;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
import com.nomasp.expression.R;
import com.nomasp.expression.crop.Crop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;


/**
 * Created by nomasp on 2016/05/17.
 */
public class RecognizeActivity extends AppCompatActivity {

    // Flag to indicate which task is to be performed.
    private static final int REQUEST_SELECT_IMAGE = 2;

    // The button to navigate to result activity.
    private Button mButtonResult;

    // The URI of the image selected to detect.
    private Uri mImageUri;
    private Uri mNewImageUri;

    // The image selected to detect.
    private Bitmap mBitmap;
    private ImageView imageView;
    // The image selected to detect.
    private EditText mEditText;

    // Spinner
    private Spinner spinnerFrom;
    private Spinner spinnerTo;
    public static String spinnerFromStr;
    public static String spinnerToStr;

    private VisionServiceClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);

        if (client == null) {
            client = new VisionServiceRestClient(getString(R.string.subscription_key));
        }

        mButtonResult = (Button) findViewById(R.id.buttonSelectImage);
        mEditText = (EditText) findViewById(R.id.editTextResult);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        mImageUri = uri;

        mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(mImageUri, getContentResolver());
        if (mBitmap != null) {
            // Show the image on screen.
            imageView = (ImageView) findViewById(R.id.selectedImage);
            imageView.setImageBitmap(mBitmap);

            // Add detection log.
            Log.d("AnalyzeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                    + "x" + mBitmap.getHeight());

            doRecognize();
        }

        spinnerFrom = (Spinner) findViewById(R.id.spinnerFrom);
        spinnerTo = (Spinner) findViewById(R.id.spinnerTo);
        spinnerFromStr = "Auto Detect";
        spinnerToStr = "Chinese Simplified";
        spinnerFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                spinnerFromStr = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinnerTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                spinnerToStr = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }
    }

    public void cropBitmap(View view) {
        beginCrop(mImageUri);
    }

    private void beginCrop(Uri source) {
        //Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped"));
        Random random = new Random();
        mNewImageUri = Uri.fromFile(new File(getCacheDir(), "cropped" + String.valueOf(random.nextInt())));
        Crop.of(source, mNewImageUri).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            mNewImageUri = Crop.getOutput(result);
            imageView.setImageURI(mNewImageUri);
            mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(mNewImageUri, getContentResolver());
            doRecognize();
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    public void selectImage(View view) {
        Intent intent = new Intent(RecognizeActivity.this, ResultActivity.class);
        Bundle bundle = new Bundle();
        if (mNewImageUri == null)
            mNewImageUri = mImageUri;
        bundle.putParcelable("KK", mImageUri);
        bundle.putParcelable("KKK", mNewImageUri);
        intent.putExtra("DD", bundle);
        //intent.setData(mImageUri);
        intent.putExtra("translator", mEditText.getText().toString());
        startActivity(intent);
    }

    public void doRecognize() {
        mButtonResult.setEnabled(false);
        mEditText.setText("Analyzing...");

        try {
            new doRequest().execute();
        } catch (Exception e) {
            mEditText.setText("Error encountered. Exception is: " + e.toString());
        }
    }

    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        OCR ocr;
        ocr = this.client.recognizeText(inputStream, LanguageCodes.AutoDetect, true);

        String result = gson.toJson(ocr);
        Log.d("result", result);

        return result;
    }

    private class doRequest extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;

        public doRequest() {
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                return process();
            } catch (Exception e) {
                this.e = e;    // Store error
            }

            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            // Display based on error existence

            if (e != null) {
                mEditText.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                Gson gson = new Gson();
                OCR r = gson.fromJson(data, OCR.class);

                String result = "";
                for (Region reg : r.regions) {
                    for (Line line : reg.lines) {
                        for (Word word : line.words) {
                            result += word.text + " ";
                        }
                        result += "\n";
                    }
                    result += "\n\n";
                }

                mEditText.setText(result);
            }
            mButtonResult.setEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
