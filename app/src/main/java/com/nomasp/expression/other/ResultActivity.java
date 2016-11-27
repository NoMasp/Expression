package com.nomasp.expression.other;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.nomasp.expression.R;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by nomasp on 2016/05/17.
 */
public class ResultActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {

    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.9f;
    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS = 0.3f;
    private static final int ALPHA_ANIMATIONS_DURATION = 200;

    private boolean mIsTheTitleVisible = false;
    private boolean mIsTheTitleContainerVisible = true;

    private LinearLayout mTitleContainer;
    private TextView mTitle;
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;

    // Add by myself
    private Uri uri;
    private Uri uri2;
    private String data;

    // 以前没使用的控件
    private ImageView imageView;
    private CircleImageView circleImageView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activty_result);

        Translate.setClientId("MicrosoftTrans22112");
        Translate.setClientSecret("h71T+rwpJUI5lEVSN1H5UH46CQEL9BC9D7uCl3bj+j4=");

        initial();
        mAppBarLayout.addOnOffsetChangedListener(this);


        Intent intent = getIntent();

        //uri = intent.getData();
        Bundle bundle = intent.getBundleExtra("DD");
        uri = bundle.getParcelable("KK");
        uri2 = bundle.getParcelable("KKK");

        data = intent.getStringExtra("translator");

        final Handler myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                data = bundle.getString("YY");
                bindActivity();

                //mToolbar.inflateMenu(R.menu.menu_main);
                startAlphaAnimation(mTitle, 0, View.INVISIBLE);
            }
        };

        new Thread() {
            public void run() {
                try {
                    String translatedText = Translate.execute(data, getLanguage(RecognizeActivity.spinnerFromStr), getLanguage(RecognizeActivity.spinnerToStr));
                    Log.i("LOG", translatedText);
                    Looper.prepare();
                    Bundle bundle = new Bundle();
                    bundle.putString("YY", translatedText);
                    Message message = Message.obtain();
                    message.setData(bundle);
                    myHandler.sendMessage(message);
                    Looper.loop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private Language getLanguage(String language) {
        switch (language) {
            case "Auto Detect":
                return Language.AUTO_DETECT;
            case "English":
                return Language.ENGLISH;
            case "Chinese Simplified":
                return Language.CHINESE_SIMPLIFIED;
            case "Chinese Traditional":
                return Language.CHINESE_TRADITIONAL;
            case "French":
                return Language.FRENCH;
            case "German":
                return Language.GERMAN;
            case "Greek":
                return Language.GREEK;
            case "Italian":
                return Language.ITALIAN;
            case "Japanese":
                return Language.JAPANESE;
            case "Korean":
                return Language.KOREAN;
            case "Russian":
                return Language.RUSSIAN;
            case "Spanish":
                return Language.SPANISH;
            case "Swedish":
                return Language.SWEDISH;
        }
        return Language.AUTO_DETECT;
    }

    private void initial() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mTitle = (TextView) findViewById(R.id.textViewTitle);
        mTitleContainer = (LinearLayout) findViewById(R.id.linearLayoutTitle);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);

        imageView = (ImageView) findViewById(R.id.imageViewPlaceholder);
        circleImageView = (CircleImageView) findViewById(R.id.circleImageView);
    }


    private void bindActivity() {

        if (uri != null) {
            System.out.println("Uri is not null.");
            Bitmap bitmap = getBitmapFromUri(uri);
            if (bitmap != null) {
                System.out.println("Bitmap is not null.");
                imageView.setImageBitmap(bitmap);
                //circleImageView.setImageBitmap(bitmap);
            }
        }

        if (uri2 != null) {
            System.out.println("Uri is not null.");
            Bitmap bitmap = getBitmapFromUri(uri2);
            if (bitmap != null) {
                System.out.println("Bitmap is not null.");
                //imageView.setImageBitmap(bitmap);
                circleImageView.setImageBitmap(bitmap);
            }
        }

        textView = (TextView) findViewById(R.id.lorem);
        textView.setText(data);
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }*/

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        handleAlphaOnTitle(percentage);
        handleToolbarTitleVisibility(percentage);
    }

    private void handleToolbarTitleVisibility(float percentage) {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {
            if (!mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleVisible = true;
            }
        } else {
            if (mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleVisible = false;
            }
        }
    }

    private void handleAlphaOnTitle(float percentage) {
        if (percentage >= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if (mIsTheTitleContainerVisible) {
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleContainerVisible = false;
            }
        } else {
            if (!mIsTheTitleContainerVisible) {
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleContainerVisible = true;
            }
        }
    }

    public static void startAlphaAnimation(View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }
}
