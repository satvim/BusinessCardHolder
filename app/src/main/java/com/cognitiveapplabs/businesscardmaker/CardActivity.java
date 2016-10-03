package com.cognitiveapplabs.businesscardmaker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.UUID;

import utils.SystemUiHider;

public class CardActivity extends Activity {

    /**
     * The whole view of the card, which we will use to obtain a 'screenshot'
     */
    private FrameLayout cardViewBackground;
    private FrameLayout cardView;
    private TextView starter;
    private RelativeLayout contentView;

    private int orientation;
    private boolean blank;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private static Context contextS;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    private int REQUEST_ENABLE_BT=1;
    private ArrayAdapter<String> mArrayAdapter;
    private ImageView image;
    private BluetoothAdapter mBluetoothAdapter = null;
    static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    static String address = "50:C3:00:00:00:00";
    private byte[] buffer = new byte[8192];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        mArrayAdapter = new ArrayAdapter<String>(this,R.layout.activity_card);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        contentView = (RelativeLayout) findViewById(R.id.fullscreen_content);
        cardViewBackground = (FrameLayout) findViewById(R.id.the_card);
        starter = (TextView) findViewById(R.id.start_text);

        prefs = this.getSharedPreferences(getResources().getString(R.string.PREFS_KEY), MODE_PRIVATE);
        editor = prefs.edit();

        orientation = prefs.getInt("orientation", 0);
        blank = prefs.getBoolean("blank", true);

        if (blank == true){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        else if (orientation == R.id.radio_land){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        else if (orientation == 0) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        //tells if the card is blank or not created

        if (blank){
            showGetStarted();
        }
        else {
            try {
                displayCard();
            } catch (IOException e) {
                Log.v("Error","Error");
            }
        }

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
            // Cached values.
            int mControlsHeight;
            int mShortAnimTime;

            @Override
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
            public void onVisibilityChange(boolean visible) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                    // If the ViewPropertyAnimator API is available
                    // (Honeycomb MR2 and later), use it to animate the
                    // in-layout UI controls at the bottom of the
                    // screen.
                    if (mControlsHeight == 0) {
                        mControlsHeight = controlsView.getHeight();
                    }
                    if (mShortAnimTime == 0) {
                        mShortAnimTime = getResources().getInteger(
                                android.R.integer.config_shortAnimTime);
                    }
                    controlsView
                            .animate()
                            .translationY(visible ? 0 : mControlsHeight)
                            .setDuration(mShortAnimTime);
                } else {
                    // If the ViewPropertyAnimator APIs aren't
                    // available, simply show or hide the in-layout UI
                    // controls.
                    controlsView.setVisibility(visible ? View.VISIBLE
                            : View.GONE);
                }
                if (visible && AUTO_HIDE) {
                    // Schedule a hide().
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }
        });
    }

    /**
     * Tells the user to get started
     */
    private void showGetStarted() {

        cardViewBackground.setBackgroundColor(getResources().getColor(R.color.blank_color));
        starter.setVisibility(View.VISIBLE);
        //cardView.setVisibility(View.GONE);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    /**
     * Starts the card building process
     * @throws IOException
     */
    private void displayCard() throws IOException {

        //First make the foundation of the card
        cardView = new FrameLayout(this);

        //Then load the desired theme and background
        cardView.setBackgroundColor(android.graphics.Color.WHITE);

        //Then compute the size of the card
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int containerW = size.x;
        int containerH = size.y;
        Log.v("Displaying Card", "Your background: Width(" + containerW + ")  Height(" + containerH +")");

        if(orientation == R.id.radio_land){
            int cardHeight = containerH - 265;
            int cardWidth = (int) (cardHeight * 1.75);
            cardView.setLayoutParams(new RelativeLayout.LayoutParams(cardWidth, cardHeight));
            Log.v("Displaying Card", "Your card is: Width(" + cardWidth + ")  Height(" + cardHeight +")");
        }
        else{
            int cardHeight = containerH - 265;
            int cardWidth = (int) (cardHeight / 1.75);
            cardView.setLayoutParams(new RelativeLayout.LayoutParams(cardWidth, cardHeight));
            Log.v("Displaying Card", "Your card is: Width(" + cardWidth + ")  Height(" + cardHeight +")");
        }

        //then compute the location of the card
        cardView.setY(cardView.getY() + 40);

        //Now create the views that will be going into the
        int layoutId = prefs.getInt("layout_id", 0);

        //Choose and create the layout
        switch(layoutId){
            case 0 : TestLayoutLand.create(cardView, contentView, prefs, this);
        }

        starter.setVisibility(View.GONE);
        cardView.setVisibility(View.VISIBLE);

        //Save file in Gallery
        File imagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/BusinessCard"); //Creates app specific folder
        imagePath.mkdirs();
        File imageFile = new File(imagePath, "MyBusinessCard.jpg");
        saveFrameLayout(cardView, imageFile);
    }

    public static Context getContext() {
        return contextS;

    }

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /**
     * Starts a sharing intent to share the image
     * @param view The view that invoked this method
     * @throws FileNotFoundException
     */
    public void shareCard(View view) throws FileNotFoundException{

        Intent intent = new Intent(this, ListDevices.class);
        startActivity(intent);
    }

    class SendData extends Thread {
        private BluetoothDevice device = null;
        private BluetoothSocket btSocket = null;
        private OutputStream outputStream = null;

        public SendData() throws FileNotFoundException {
        }

        public void sendMessage(BluetoothSocket socket) throws FileNotFoundException {

            Log.v("BusinessCardMaker", "About to create new image file");

            //First we create a file output stream to save the card
            //TODO: Put in the right directory. Hover to view more information
            File gallery = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/BusinessCard");
            File file = new File(gallery, "MyBusinessCard.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            Log.v("BusinessCardMaker", "New File Created");

            //Log.v("BusinessCardMaker", "Creating the URI");
            //Uri uri = Uri.fromFile(file);

            try {
                OutputStream os = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                try {
                    File f = new File(file.getPath());
                    long filelength = f.length();
                    byte[] bytes = new byte[(int) filelength];
                    oos.writeObject(bytes);
                    oos.flush();
                    oos.close();
                    Log.v("BusinessCardMaker", "Finished and succeeded");
                } catch (Exception e){
                    //e.printStackTrace();
                    Log.v("BusinessCardMaker", e.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
                //Log.v("BusinessCardMaker", e.toString());
            }
        }
    }

    public void saveFrameLayout(FrameLayout frameLayout, File path) {
        frameLayout.setDrawingCacheEnabled(true);
        frameLayout.buildDrawingCache(true);
        Bitmap cache = Bitmap.createBitmap(frameLayout.getLayoutParams().width, frameLayout.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        //frameLayout.setDrawingCacheEnabled(false);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            cache.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.getFD().sync();
            fileOutputStream.close();

            MediaScannerConnection.scanFile(this,
                    new String[]{path.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("Storage", "Scanned " + path + ":");
                            Log.i("Storage", "-> uri=" + uri);
                        }
                    });

        } catch (Exception e) {
            Log.v("BusinessCardMaker", e.toString());
        } finally {
            frameLayout.destroyDrawingCache();
        }
    }
    /**
     * Opens the edit info page
     * @param view The view that invoked this method
     */
    public void editInfo(View view){

        Intent intent = new Intent(this, EditActivity.class);
        startActivity(intent);
        this.finish();

    }
}
