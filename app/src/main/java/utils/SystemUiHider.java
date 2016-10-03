package utils;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.view.View;

/**
 * Created by saanton on 9/19/2016.
 *
 * A utility class that helps with showing and hiding system UI such as the
 * status bar and navigation/system bar.
 */
public abstract class SystemUiHider {

    /**
     * When this flag is set, the
     * {@link android.view.WindowManager.LayoutParams#FLAG_LAYOUT_IN_SCREEN}
     * flag will be set on older devices, making the status bar "float" on top
     * of the activity layout. This is most useful when there are no controls at
     * the top of the activity layout.
     * <p>
     * This flag isn't used on newer devices because the <a
     * href="http://developer.android.com/design/patterns/actionbar.html">action
     * bar</a>, the most important structural element of an Android app, should
     * be visible and not obscured by the system UI.
     */
    public static final int FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES = 0x1;

    /**
     * When this flag is set, {@link #show()} and {@link #hide()} will toggle
     * the visibility of the status bar. If there is a navigation bar, show and
     * hide will toggle low profile mode.
     */
    public static final int FLAG_FULLSCREEN = 0x2;

    /**
     * When this flag is set, {@link #show()} and {@link #hide()} will toggle
     * the visibility of the navigation bar, if it's present on the device and
     * the device allows hiding it. In cases where the navigation bar is present
     * but cannot be hidden, show and hide will toggle low profile mode.
     */
    public static final int FLAG_HIDE_NAVIGATION = FLAG_FULLSCREEN | 0x4;

    /**
     * The activity associated with this UI hider object.
     */
    protected Activity mActivity;

    /**
     * The view on which {@link View#setSystemUiVisibility(int)} will be called.
     */
    protected View mAnchorView;

    /**
     * The current UI hider flags.
     *
     * @see #FLAG_FULLSCREEN
     * @see #FLAG_HIDE_NAVIGATION
     * @see #FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES
     */
    protected int mFlags;

    /**
     * The current visibility callback.
     */
    protected OnVisibilityChangeListener mOnVisibilityChangeListener = sDummyListener;

    //Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE =1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */

    /*public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }*/


    protected SystemUiHider(Activity activity, View anchorView, int flags) {
        mActivity = activity;
        mAnchorView = anchorView;
        mFlags = flags;
    }


    public static SystemUiHider getInstance(Activity activity, View view, int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new SystemUiHiderHoneycomb(activity, view, flags);
        }
        else {
            return new SystemUiHiderBase(activity,view,flags);
        }
    }

    /**
     * Sets up the system UI hider. Should be called from
     * {@link Activity#onCreate}.
     */
    public abstract void setup();

    /**
     * Hide the system UI.
     */
    public abstract void hide();

    /**
     * A dummy no-op callback for use when there is no other listener set.
     */
    private static OnVisibilityChangeListener sDummyListener = new OnVisibilityChangeListener() {
        @Override
        public void onVisibilityChange(boolean visible) {
        }
    };

    /**
     * Returns whether or not the system UI is visible.
     */
    public abstract boolean isVisible();

    /**
     * Show the system UI.
     */
    public abstract void show();

    /**
     * A callback interface used to listen for system UI visibility changes.
     */
    public interface OnVisibilityChangeListener {
        /**
         * Called when the system UI visibility has changed.
         *
         * @param visible
         *            True if the system UI is visible.
         */
        public void onVisibilityChange(boolean visible);
    }

    /**
     * Registers a callback, to be triggered when the system UI visibility
     * changes.
     */
    public void setOnVisibilityChangeListener(
            OnVisibilityChangeListener listener) {
        if (listener == null) {
            listener = sDummyListener;
        }

        mOnVisibilityChangeListener = listener;
    }
}
