package com.xxf.arch.utils;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.core.app.NotificationManagerCompat;

import com.xxf.arch.R;
import com.xxf.arch.XXF;
import com.xxf.view.snackbar.Snackbar;

import java.lang.reflect.Field;

/**
 * Description
 * * <p>
 * 【不建议使用 不建议使用】
 * <p>
 * 某些系统可能屏蔽通知
 * 1:检查 SystemUtils.isEnableNotification(BaseApplication.getApplication());
 * 2:替代方案 SnackbarUtils.showSnack(topActivity, noticeStr);
 * <p>
 * <p>
 * author  youxuan  E-mail:xuanyouwu@163.com
 * date createTime：2017/4/27
 * version 1.0.0
 */
public class ToastUtils {
    public enum ToastType {
        NORMAL,
        ERROR,
        SUCCESS;
    }

    private static Field sField_TN;
    private static Field sField_TN_Handler;

    private ToastUtils() {
    }

    /**
     * 通过反射封装 Toast 类中TN Binder内部类中的handler,
     * 捕获BadTokenException, 解决Android API 25 引入的
     * Bug
     */
    static {
        try {
            sField_TN = Toast.class.getDeclaredField("mTN");
            sField_TN.setAccessible(true);
            sField_TN_Handler = sField_TN.getType().getDeclaredField("mHandler");
            sField_TN_Handler.setAccessible(true);
        } catch (Exception e) {
        }
    }

    public static void hook(Toast toast) {
        try {
            Object tn = sField_TN.get(toast);
            Handler preHandler = (Handler) sField_TN_Handler.get(tn);
            sField_TN_Handler.set(tn, new SafelyHandlerWrapper(preHandler));
        } catch (Exception e) {
        }
    }


    private static class SafelyHandlerWrapper extends Handler {

        private Handler impl;

        public SafelyHandlerWrapper(Handler impl) {
            this.impl = impl;
        }

        @Override
        public void dispatchMessage(Message msg) {
            try {
                super.dispatchMessage(msg);
            } catch (Exception e) {
            }
        }

        @Override
        public void handleMessage(Message msg) {
            impl.handleMessage(msg);//需要委托给原Handler执行
        }
    }


    private static Context getContext() {
        return XXF.getApplication();
    }

    /**
     * toast是否可用
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static boolean isToastAvailable() {
        try {
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getContext());
            boolean areNotificationsEnabled = notificationManagerCompat.areNotificationsEnabled();
            return areNotificationsEnabled;
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         * 默认可用
         */
        return true;
    }

    /**
     * 校验线程
     * 否则:Can't create handler inside thread that has not called Looper.prepare()
     *
     * @param notice
     * @return
     */
    @UiThread
    @Nullable
    public static Toast showToast(@StringRes int notice, @NonNull ToastType type) {
        return showToast(XXF.getApplication().getString(notice), type);
    }

    /**
     * 校验线程
     * 否则:Can't create handler inside thread that has not called Looper.prepare()
     *
     * @param notice
     * @return
     */
    @UiThread
    @Nullable
    public static Toast showToast(@NonNull CharSequence notice, @NonNull ToastType type) {
        if (!isMainThread() || TextUtils.isEmpty(notice)) {
            return null;
        }
        //app 后台不允许toast
        if (XXF.getActivityStackProvider().isBackground()) {
            return null;
        }

        //如果系统禁用Toast,用SnackBar替代
        if (!isToastAvailable()) {
            Activity topActivity = XXF.getActivityStackProvider().getTopActivity();
            showSnackBar(topActivity, notice, type);
            return null;
        }
        Toast toast = createToast(notice, type);
        //fix bug #65709 BadTokenException from BugTags
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
            hook(toast);
        }
        toast.show();
        return toast;
    }

    public static void showSnackBar(@NonNull Activity topActivity, @NonNull CharSequence notice, @NonNull ToastType type) {
        if (topActivity != null && !topActivity.isDestroyed() && !topActivity.isFinishing()) {
            Snackbar snackbar = Snackbar.make(topActivity.getWindow().getDecorView(), notice, Snackbar.LENGTH_SHORT);
            View snackbarView = snackbar.getView();
            try {
                int statusBarHeight = getStatusBarHeight(topActivity);
                snackbarView.setPadding(0, statusBarHeight, 0, 0);
                snackbarView.setBackgroundColor(0xFF333333);
                TextView textView = (TextView) snackbarView.findViewById(com.xxf.view.snackbar.R.id.snackbar_text);
                textView.setTextColor(Color.WHITE);
                textView.setMaxLines(3);
                textView.setCompoundDrawablePadding(DensityUtil.dip2px(7));
                int dp19 = DensityUtil.dip2px(19);
                switch (type) {
                    case ERROR:
                        Drawable errorDrawable = XXF.getApplication().getDrawable(R.drawable.xxf_ic_toast_error);
                        errorDrawable.setBounds(0, 0, dp19, dp19);
                        textView.setCompoundDrawables(errorDrawable, null, null, null);
                        break;
                    case NORMAL:
                        textView.setCompoundDrawables(null, null, null, null);
                        break;
                    case SUCCESS:
                        Drawable successDrawable = XXF.getApplication().getDrawable(R.drawable.xxf_ic_toast_success);
                        successDrawable.setBounds(0, 0, dp19, dp19);
                        textView.setCompoundDrawables(successDrawable, null, null, null);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            snackbar.show();
        }
    }

    /**
     * 获取状态栏高度
     *
     * @param context context
     * @return 状态栏高度
     */
    private static int getStatusBarHeight(Context context) {
        // 获得状态栏高度
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return context.getResources().getDimensionPixelSize(resourceId);
    }

    public static Toast createToast(CharSequence msg, ToastType type) {
        LayoutInflater inflater = LayoutInflater.from(XXF.getApplication());
        View view = inflater.inflate(R.layout.xxf_toast_layout, null);

        TextView text = view.findViewById(android.R.id.message);
        int dp19 = DensityUtil.dip2px(19);
        switch (type) {
            case ERROR:
                Drawable errorDrawable = XXF.getApplication().getDrawable(R.drawable.xxf_ic_toast_error);
                errorDrawable.setBounds(0, 0, dp19, dp19);
                text.setCompoundDrawables(errorDrawable, null, null, null);
                break;
            case NORMAL:
                text.setCompoundDrawables(null, null, null, null);
                break;
            case SUCCESS:
                Drawable successDrawable = XXF.getApplication().getDrawable(R.drawable.xxf_ic_toast_success);
                successDrawable.setBounds(0, 0, dp19, dp19);
                text.setCompoundDrawables(successDrawable, null, null, null);
                break;
        }
        text.setText(msg);
        Toast toast = new Toast(XXF.getApplication());
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        return toast;
    }


    /**
     * 检查是否在主线程showToast
     *
     * @return
     */
    private static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
