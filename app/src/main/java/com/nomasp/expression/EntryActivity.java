package com.nomasp.expression;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
import com.nomasp.expression.fragment.MainFragment;
import com.nomasp.expression.fragment.PictureToText;
import com.nomasp.expression.other.ImageHelper;
import com.nomasp.libs.ContextMenuDialogFragment;
import com.nomasp.libs.MenuObject;
import com.nomasp.libs.MenuParams;
import com.nomasp.libs.OnMenuItemClickListener;
import com.nomasp.libs.OnMenuItemLongClickListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nomasp on 16-11-27.
 */

public class EntryActivity extends AppCompatActivity implements OnMenuItemClickListener, OnMenuItemLongClickListener {

    // Something for "Picture to Text"
    // The uri of picture taken with camera
    private Uri mUriPictureTaken;
    // Flag to indicate the request of the next task to be performed
    private static final int REQUEST_TAKE_A_PICTURE = 1200;
    private static final int REQUEST_GET_FROM_GALLERY = 1201;
    // The uri of picture that app get last
    private Uri mUriImageLast;


    // Something for recognizing image
    // Computer Vision Client
    private VisionServiceClient mVisionServiceClient;
    // The uri of the selected image to detect
    private Uri mImageUri;
    private Uri mNewImageUri;
    // The bitmap for recognizing
    private Bitmap mRecognizeBtp;
    // To show the image which user chooses
    private ImageView mShowPicture;
    // To show the text from image
    private TextView mShowText;
    // Tools button
    private Button btnRecognizedEdit;
    private Button btnRecognizedCopy;
    // View for edit dialog
    View dialogLayout;

    // Something for "Translate"
    // Text that needs to translate
    private String preTranslator;
    private String afterTranslator;
    // Tools button
    private Button btnTranslatedEdit;
    private Button btnTranslatedCopy;
    // To show the translated text
    private TextView translatedText;
    // Some static language name for translator
    private static String[] languageName = new String[]{
            "English", "Chinese Simplified", "Chinese Traditional", "French", "German", "Greek",
            "Italian", "Japanese", "Korean", "Russian", "Spanish", "Swedish"};

    private FragmentManager fragmentManager;
    private ContextMenuDialogFragment mMenuDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        initComputerVisionClient();
        initTranslateClient();

        initRecognizedController();
        initTranslatedController();

        fragmentManager = getSupportFragmentManager();
        initToolbar();
        initMenuFragment();
        addFragment(new MainFragment(), true, R.id.container);
    }

    @Override
    protected void onStart() {
        super.onStart();

        initRecognizedController();
        initTranslatedController();
    }

    private void initComputerVisionClient() {
        if (mVisionServiceClient == null) {
            mVisionServiceClient = new VisionServiceRestClient(getString(R.string.subscription_key));
        }
    }

    private void initTranslateClient() {
        Translate.setClientId("MicrosoftTrans22112");
        Translate.setClientSecret("h71T+rwpJUI5lEVSN1H5UH46CQEL9BC9D7uCl3bj+j4=");
    }

    private void initRecognizedController() {
        mShowPicture = (ImageView) findViewById(R.id.recognizedImage);
        mShowText = (TextView) findViewById(R.id.recognizedText);
        btnRecognizedEdit = (Button) findViewById(R.id.btnRecognizedEdit);
        btnRecognizedCopy = (Button) findViewById(R.id.btnRecognizedCopy);

        if (btnRecognizedEdit != null) {
            btnRecognizedEdit.setVisibility(View.INVISIBLE);
            btnRecognizedEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LayoutInflater inflater = getLayoutInflater();
                    dialogLayout = inflater.inflate(R.layout.alertdialog_recognizedtext_edit,
                            (ViewGroup) findViewById(R.id.dialogEditorLayout));
                    final EditText editText = (EditText) dialogLayout.findViewById(R.id.dialogEditText);
                    editText.setText(mShowText.getText());
                    new AlertDialog.Builder(EntryActivity.this)
                            .setTitle("Now you can edit your text")
                            .setView(dialogLayout)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mShowText.setText(editText.getText());
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                }
            });
        }
        if (btnRecognizedCopy != null) {
            btnRecognizedCopy.setVisibility(View.INVISIBLE);
            btnRecognizedCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("com.nomasp.expression", mShowText.getText().toString());
                    clipboardManager.setPrimaryClip(clipData);
                }
            });
        }
    }

    private void initTranslatedController() {
        translatedText = (TextView) findViewById(R.id.translatedText);
        btnTranslatedEdit = (Button) findViewById(R.id.btnTranslatedEdit);
        btnTranslatedCopy = (Button) findViewById(R.id.btnTranslatedCopy);
        if (btnTranslatedEdit != null) {
            btnTranslatedEdit.setVisibility(View.INVISIBLE);
            btnTranslatedEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LayoutInflater inflater = getLayoutInflater();
                    dialogLayout = inflater.inflate(R.layout.alertdialog_recognizedtext_edit,
                            (ViewGroup) findViewById(R.id.dialogEditorLayout));
                    final EditText editText = (EditText) dialogLayout.findViewById(R.id.dialogEditText);
                    editText.setText(mShowText.getText());
                    new AlertDialog.Builder(EntryActivity.this)
                            .setTitle("Now you can edit your text")
                            .setView(dialogLayout)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    translatedText.setText(editText.getText());
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                }
            });
        }
        if (btnTranslatedCopy != null) {
            btnTranslatedCopy.setVisibility(View.INVISIBLE);
            btnTranslatedCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("com.nomasp.expression", translatedText.getText().toString());
                    clipboardManager.setPrimaryClip(clipData);
                }
            });
        }
    }

    private void initMenuFragment() {
        MenuParams menuParams = new MenuParams();
        menuParams.setActionBarSize((int) getResources().getDimension(R.dimen.tool_bar_height));
        menuParams.setMenuObjects(getMenuObjects());
        menuParams.setClosableOutside(false);
        mMenuDialogFragment = ContextMenuDialogFragment.newInstance(menuParams);
        mMenuDialogFragment.setItemClickListener(this);
        mMenuDialogFragment.setItemLongClickListener(this);
    }

    private List<MenuObject> getMenuObjects() {
        // You can use any [resource, bitmap, drawable, color] as image:
        // item.setResource(...)
        // item.setBitmap(...)
        // item.setDrawable(...)
        // item.setColor(...)
        // You can set image ScaleType:
        // item.setScaleType(ScaleType.FIT_XY)
        // You can use any [resource, drawable, color] as background:
        // item.setBgResource(...)
        // item.setBgDrawable(...)
        // item.setBgColor(...)
        // You can use any [color] as text color:
        // item.setTextColor(...)
        // You can set any [color] as divider color:
        // item.setDividerColor(...)

        List<MenuObject> menuObjects = new ArrayList<>();

        MenuObject close = new MenuObject();
        close.setResource(R.drawable.icn_close);

        MenuObject send = new MenuObject("Picture to Text");
        // send.setResource(R.drawable.icn_1);
        send.setResource(R.drawable.picture);

        MenuObject like = new MenuObject("Voice to Text");
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.icn_2);
        //like.setBitmap(b);
        like.setResource(R.drawable.speak);

        MenuObject addFr = new MenuObject("Text to Voice");
        //BitmapDrawable bd = new BitmapDrawable(getResources(),
        //        BitmapFactory.decodeResource(getResources(), R.drawable.icn_3));
        BitmapDrawable bd = new BitmapDrawable(getResources(),
                BitmapFactory.decodeResource(getResources(), R.drawable.sound));
        addFr.setDrawable(bd);

        MenuObject addFav = new MenuObject("Translate Text");
        // addFav.setResource(R.drawable.icn_4);
        addFav.setResource(R.drawable.translate);

        MenuObject block = new MenuObject("");
        block.setResource(R.drawable.icn_5);

        menuObjects.add(close);
        menuObjects.add(send);
        menuObjects.add(like);
        menuObjects.add(addFr);
        menuObjects.add(addFav);
        menuObjects.add(block);
        return menuObjects;
    }

    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView mToolBarTextView = (TextView) findViewById(R.id.text_view_toolbar_title);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mToolbar.setNavigationIcon(R.drawable.btn_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mToolBarTextView.setText("Expression");
    }

    protected void addFragment(Fragment fragment, boolean addToBackStack, int containerId) {
        invalidateOptionsMenu();
        String backStackName = fragment.getClass().getName();
        boolean fragmentPopped = fragmentManager.popBackStackImmediate(backStackName, 0);
        if (!fragmentPopped) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(containerId, fragment, backStackName)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            //.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            if (addToBackStack)
                transaction.addToBackStack(backStackName);
            transaction.commit();
        }
    }

/*    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.context_menu:
                if (fragmentManager.findFragmentByTag(ContextMenuDialogFragment.TAG) == null) {
                    mMenuDialogFragment.show(fragmentManager, ContextMenuDialogFragment.TAG);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public void onBackPressed() {
        if (mMenuDialogFragment != null && mMenuDialogFragment.isAdded()) {
            mMenuDialogFragment.dismiss();
        } else{
            finish();
        }
    }

    // Use index to indicate which item is clicked
    int indexForPictureToText = -1;
    int indexForTranslate = -1;
    @Override
    public void onMenuItemClick(View clickedView, int position) {
        // which position is clicked
        // Toast.makeText(this, position + "", Toast.LENGTH_SHORT).show();
        switch (position) {
            // picture to text
            case 1:
                new AlertDialog.Builder(this).setTitle("Which method would you want Expression to use ?")
                        .setSingleChoiceItems(new String[]{"Take a picture", "Get from gallery"}, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Toast.makeText(MainActivity.this, which + "", Toast.LENGTH_SHORT).show();
                                indexForPictureToText = which;
                            }
                        })
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PictureToText(indexForPictureToText);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(EntryActivity.this, "CC", Toast.LENGTH_SHORT).show();
                            }
                        }).show();
                break;
            // voice to text
            case 2:
                break;
            // text to voice
            case 3:
                if (mShowText == null || mShowText.getText().toString().equals("")) {
                    makeToast("Sorry, if there aren't text, I can't speek for you.\nBut you can try these features:\nPicture to Text, Voice To Texe.", true);
                } else {

                }
                break;
            // tranlate
            case 4:
                if (mShowText == null || mShowText.getText().toString().equals("")) {
                    makeToast("Sorry, you haven't some text to translate.\nBut you can try these features:\nPicture to Text, Voice To Texe.", true);
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Choose a language which you want to translate to")
                            .setSingleChoiceItems(languageName, -1, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Toast.makeText(MainActivity.this, which + "", Toast.LENGTH_SHORT).show();
                                    indexForTranslate = which;
                                }
                            })
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    preTranslator = mShowText.getText().toString();
                                    GoToTranslate(indexForTranslate);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(EntryActivity.this, "AA", Toast.LENGTH_SHORT).show();
                                }
                            }).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onMenuItemLongClick(View clickedView, int position) {
        Toast.makeText(this, "Long clicked on position: " + position, Toast.LENGTH_SHORT).show();
    }

    private void PictureToText(int which) {
        replaceFragment(new PictureToText());
        // 0 means "take a picture", 1 means "get from gallery".
        switch (which) {
            case -1:
                break;
            case 0:
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Save the picture to a temporary file.
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    try {
                        File file = File.createTempFile("IMG_", ".jpg", storageDir);
                        mUriPictureTaken = Uri.fromFile(file);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPictureTaken);
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_A_PICTURE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 1:
                Intent getFromGalleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getFromGalleryIntent.setType("image/*");
                if (getFromGalleryIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(getFromGalleryIntent, REQUEST_GET_FROM_GALLERY);
                }
                break;
            default:
                break;
        }
    }

    private void GoToTranslate(final int indexLanguage) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String translated = Translate.execute(preTranslator, getLanguage(-1), getLanguage(indexLanguage));
                    Bundle bundle = new Bundle();
                    bundle.putString("TranslatedText", translated);
                    Message message = Message.obtain();
                    message.setData(bundle);
                    translatedHandler.sendMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    final Handler translatedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            afterTranslator = bundle.getString("TranslatedText");
            btnTranslatedEdit.setVisibility(View.VISIBLE);
            btnTranslatedCopy.setVisibility(View.VISIBLE);
            translatedText.setText(afterTranslator);
        }
    };

    private Language getLanguage(int indexLanguage) {
        switch (indexLanguage) {
            case -1:
                return Language.AUTO_DETECT;
            case 0:
                return Language.ENGLISH;
            case 1:
                return Language.CHINESE_SIMPLIFIED;
            case 2:
                return Language.CHINESE_TRADITIONAL;
            case 3:
                return Language.FRENCH;
            case 4:
                return Language.GERMAN;
            case 5:
                return Language.GREEK;
            case 6:
                return Language.ITALIAN;
            case 7:
                return Language.JAPANESE;
            case 8:
                return Language.KOREAN;
            case 9:
                return Language.RUSSIAN;
            case 10:
                return Language.SPANISH;
            case 11:
                return Language.SWEDISH;
            default:
                break;
        }
        return Language.AUTO_DETECT;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_A_PICTURE:
            case REQUEST_GET_FROM_GALLERY:
                initRecognizedController();
                initTranslatedController();
                if (resultCode == RESULT_OK) {
                    if (data == null || data.getData() == null) {
                        mUriImageLast = mUriPictureTaken;
                    } else {
                        mUriImageLast = data.getData();
                    }
                    mImageUri = mUriImageLast;
                    mRecognizeBtp =  resizeBitmapFromUri(mImageUri);
                    loadImageView(mRecognizeBtp);
                    doRecognize();
                }
                break;
            default:
                break;
        }
    }

    // Save the activity state when it's going to stop.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ImageUri", mUriPictureTaken);
    }

    // Recover the saved state when the activity is recreated.
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mUriPictureTaken = savedInstanceState.getParcelable("ImageUri");
    }


    // Something from original code : RecognizeActivity.java
    private Bitmap resizeBitmapFromUri(Uri uri) {
        Bitmap bitmap = ImageHelper.loadSizeLimitedBitmapFromUri(uri, getContentResolver());
        return bitmap;
    }

    private void loadImageView(Bitmap bitmap) {
        if (bitmap != null) {
            mShowPicture.setImageBitmap(bitmap);
        }
    }

    // Start to recognize the picture which user chooses.
    private void doRecognize() {
        mShowText.setText("Analyzing...\nPlease wait a sec...");

        try {
            new DoRequest().execute();
        } catch (Exception e) {
            mShowText.setText("Sorry, we met some error.\n" + e.getMessage());
        }
    }

    private class DoRequest extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;

        public DoRequest() {

        }

        @Override
        protected String doInBackground(String... params) {
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
                mShowText.setText("Sorry, we met some error.\n" + e.getMessage());
                this.e = null;
            } else {
                Gson gson = new Gson();
                OCR ocr = gson.fromJson(data, OCR.class);

                StringBuilder result = new StringBuilder();
                for (Region region : ocr.regions) {
                    for (Line line : region.lines) {
                        for (Word word : line.words) {
                            result.append(word.text);
                        }
                        result.append("\n");
                    }
                    result.append("\n\n");
                }
                mShowText.setText(result);
                if (!mShowText.equals("Analyzing...\nPlease wait a sec...")) {
                    btnRecognizedEdit.setVisibility(View.VISIBLE);
                    btnRecognizedCopy.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mRecognizeBtp.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());

        OCR ocr = this.mVisionServiceClient.recognizeText(input, LanguageCodes.AutoDetect, true);

        String result = gson.toJson(ocr);

        return result;
    }

    private String remakeRecognizedText(String result) {
        return "";
    }

    // Another things
    private boolean replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();
        return true;
    }


    private void makeToast(String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }
    private void makeToast(int toast) {
        Toast.makeText(this, toast + "", Toast.LENGTH_SHORT).show();
    }
    private void makeToast(String toast, boolean lengthLong) {
        if (lengthLong) {
            Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
        }
    }
}
