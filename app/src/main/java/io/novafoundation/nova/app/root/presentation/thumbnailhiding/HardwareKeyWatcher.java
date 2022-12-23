package io.novafoundation.nova.app.root.presentation.thumbnailhiding;

import android.content.*;
import android.util.Log;

import io.novafoundation.nova.app.BuildConfig;

/**
 * https://stackoverflow.com/questions/34471366/detect-touch-event-of-navigation-buttons-inside-a-service-having-window
 */
public class HardwareKeyWatcher {

    private static final String TAG = "HardwareKeyWatcher";
    private Context mContext;
    private IntentFilter mFilter;
    private OnHardwareKeysPressedListener mListener;
    private InnerReceiver mReceiver;

    public interface OnHardwareKeysPressedListener {
        void onHomePressed();

        void onRecentAppsPressed();
    }

    public HardwareKeyWatcher(Context context) {
        mContext = context;
        mFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mFilter.setPriority(1000);
    }

    public void setOnHardwareKeysPressedListenerListener(OnHardwareKeysPressedListener listener) {
        mListener = listener;
        mReceiver = new InnerReceiver();
    }

    public void startWatch() {
        if (mReceiver != null) {
            logForDebugging("startWatch on " + mContext);
            mContext.registerReceiver(mReceiver, mFilter);
        }
    }

    public void stopWatch() {
        if (mReceiver != null) {
            logForDebugging("stopWatch on " + mContext);
            mContext.unregisterReceiver(mReceiver);
        }
    }

    public Context getContext() {
        return mContext;
    }

    class InnerReceiver extends BroadcastReceiver {

        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
        final String SYSTEM_DIALOG_REASON_RECENT_APPS_XIAOMI = "fs_gesture";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reason != null) {
                    logForInfo("action:" + action + ", reason:" + reason);
                    if (mListener != null) {
                        switch (reason) {
                            case SYSTEM_DIALOG_REASON_HOME_KEY:
                                logForDebugging("onHomePressed (homekey)");
                                mListener.onHomePressed();
                                break;
                            case SYSTEM_DIALOG_REASON_RECENT_APPS:
                                logForDebugging("onRecentAppsPressed (recentapps)");
                                mListener.onRecentAppsPressed();
                                break;
                            case SYSTEM_DIALOG_REASON_RECENT_APPS_XIAOMI:
                                logForDebugging("onRecentAppsPressed (fs_gesture)");
                                mListener.onRecentAppsPressed();
                                break;
                        }
                    }
                }
            }
        }
    }

    private void logForDebugging(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }

    private void logForInfo(String message) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message);
        }
    }
}
