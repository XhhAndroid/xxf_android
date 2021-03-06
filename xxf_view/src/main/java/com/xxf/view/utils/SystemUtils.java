package com.xxf.view.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.xxf.arch.XXF;
import com.xxf.arch.core.activityresult.ActivityResult;
import com.xxf.arch.rxjava.transformer.CameraPermissionTransformer;
import com.xxf.arch.rxjava.transformer.FilePermissionTransformer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

/**
 * @Author: XGod  xuanyouwu@163.com  17611639080  https://github.com/NBXXF     https://blog.csdn.net/axuanqq  xuanyouwu@163.com  17611639080  https://github.com/NBXXF     https://blog.csdn.net/axuanqq
 * @Description
 */
public class SystemUtils {

    public static final int REQUEST_CODE_CAMERA = 59999;
    public static final int REQUEST_CODE_ALBUM = 59998;
    public static final int REQUEST_CODE_DOCUMENT = 59997;
    public static final int REQUEST_CODE_SHARE = 59996;


    /**
     * @return 获取随机图片文件(未实际生成)
     */
    public static File getRandomImageFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String fileName = String.format("capture_img_%s.jpg", sdf.format(new Date()));
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
    }

    /**
     * 获取拍照意图
     *
     * @param imageFile
     * @return
     */
    @RequiresPermission(allOf = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA})
    public static Intent getTakePhotoIntent(File imageFile) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT < 24) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
        } else {
            ContentValues contentValues = new ContentValues(1);
            contentValues.put("_data", imageFile.getAbsolutePath());
            Uri outImgUri = XXF.getApplication().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outImgUri);
        }
        return intent;
    }

    /**
     * 相册意图
     *
     * @return
     */
    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public static Intent getAlbumIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        return intent;
    }

    /**
     * 调用系统拍照
     * 自动请求权限 没有权限报异常 {@link com.xxf.arch.exception.PermissionDeniedException}
     *
     * @param context
     * @return
     */
    public static Observable<String> takePhoto(final FragmentActivity context) {
        return takePhoto(context, getRandomImageFile());
    }

    /**
     * 调用系统拍照
     * 自动请求权限 没有权限报异常 {@link com.xxf.arch.exception.PermissionDeniedException}
     *
     * @param context
     * @param imageFile
     * @return
     */
    public static Observable<String> takePhoto(final FragmentActivity context, final File imageFile) {
        return XXF.requestPermission(context, Manifest.permission.CAMERA)
                .take(1)
                .compose(new CameraPermissionTransformer())
                .flatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(Boolean cameraPermissionAllow) throws Exception {
                        return XXF.requestPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .take(1)
                                .compose(new FilePermissionTransformer());
                    }
                })
                .flatMap(new Function<Boolean, ObservableSource<String>>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public ObservableSource<String> apply(Boolean storageCameraPermissionAllow) throws Exception {
                        return XXF.startActivityForResult(context, getTakePhotoIntent(imageFile), REQUEST_CODE_CAMERA)
                                .take(1)
                                .filter(new Predicate<ActivityResult>() {
                                    @Override
                                    public boolean test(ActivityResult result) throws Exception {
                                        return result.isOk();
                                    }
                                })
                                .map(new Function<ActivityResult, String>() {
                                    @Override
                                    public String apply(ActivityResult activityResult) throws Exception {
                                        if (!activityResult.isOk()) {
                                            throw new RuntimeException("cancel");
                                        }
                                        return imageFile.getAbsolutePath();
                                    }
                                });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }


    /**
     * 保存图片到相册
     * 自动请求权限 没有权限报异常 {@link com.xxf.arch.exception.PermissionDeniedException}
     *
     * @param context
     * @param picName 是name 不说full path
     * @param bmp
     * @return
     */
    public static Observable<File> saveImageToAlbum(FragmentActivity context, String picName, Bitmap bmp) {
        return XXF.requestPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .take(1)
                .compose(new CameraPermissionTransformer())
                .flatMap(new Function<Boolean, ObservableSource<File>>() {
                    @Override
                    public ObservableSource<File> apply(Boolean aBoolean) throws Exception {
                        return Observable
                                .fromCallable(new Callable<File>() {
                                    @Override
                                    public File call() throws Exception {
                                        File picFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), picName);
                                        FileOutputStream fos = new FileOutputStream(picFile);
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                        fos.flush();
                                        fos.close();
                                        try {
                                            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                                                    picFile.getAbsolutePath(), picName, null);
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                        // 最后通知图库更新
                                        Uri localUri = Uri.fromFile(picFile);
                                        Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
                                        context.sendBroadcast(localIntent);
                                        return picFile;
                                    }
                                })
                                .subscribeOn(Schedulers.io());
                    }
                })
                .compose(XXF.bindUntilEvent(context, Lifecycle.Event.ON_DESTROY));
    }

    /**
     * 选择相片
     * 自动请求权限 没有权限报异常 {@link com.xxf.arch.exception.PermissionDeniedException}
     *
     * @param context
     * @return
     */
    public static Observable<String> selectAlbum(final FragmentActivity context) {
        return XXF.requestPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .take(1)
                .compose(new CameraPermissionTransformer())
                .flatMap(new Function<Boolean, ObservableSource<String>>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public ObservableSource<String> apply(Boolean aBoolean) throws Exception {
                        return XXF.startActivityForResult(context, getAlbumIntent(), REQUEST_CODE_ALBUM)
                                .take(1)
                                .filter(new Predicate<ActivityResult>() {
                                    @Override
                                    public boolean test(ActivityResult result) throws Exception {
                                        return result.isOk();
                                    }
                                })
                                .map(new Function<ActivityResult, String>() {
                                    @Override
                                    public String apply(ActivityResult activityResult) throws Exception {
                                        if (!activityResult.isOk()) {
                                            throw new RuntimeException("cancel");
                                        }
                                        return UriUtils.getPath(context, activityResult.getData().getData());
                                    }
                                });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }


    private static Intent getDocumentIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        String[] supportedMimeTypes = {
                //  "application/msword",
                "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes);
        //先支持pdf
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);//多选
        return intent;
    }


    /**
     * 选择文档
     *
     * @param activity
     * @return
     */
    public static Observable<String> selectDocument(final FragmentActivity activity) {
        Intent intent = getDocumentIntent();
        return XXF.startActivityForResult(activity, intent, REQUEST_CODE_DOCUMENT)
                .map(new Function<ActivityResult, String>() {
                    @Override
                    public String apply(ActivityResult activityResult) throws Exception {
                        if (!activityResult.isOk()) {
                            throw new RuntimeException("cancel");
                        }
                        return UriUtils.getPath(activity, activityResult.getData().getData());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 隐藏软键盘
     *
     * @param act
     */
    public static void hideSoftKeyBoard(Activity act) {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(act.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
        }
    }

    /**
     * 隐藏软键盘
     *
     * @param act
     */
    public static void hideSoftKeyBoard(Activity act, boolean clearFouces) {
        try {
            if (clearFouces) {
                View currentFocus = act.getCurrentFocus();
                if (currentFocus != null) {
                    currentFocus.clearFocus();
                }
            }
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(act.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
        }
    }

    /**
     * 隐藏软键盘2
     *
     * @param act
     */
    public static void hideSoftKeyBoard(Context act, View v) {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } catch (Exception e) {
        }
    }

    /**
     * 隐藏软键盘2
     *
     * @param act
     */
    public static void hideSoftKeyBoard(Context act, View v, boolean clearFouces) {
        try {
            if (v != null && clearFouces) {
                v.clearFocus();
            }
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } catch (Exception e) {
        }
    }


    /**
     * 显示软键盘
     *
     * @param act
     */
    public static void showSoftKeyBoard(Activity act) {
        try {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(act.getCurrentFocus(), InputMethodManager.SHOW_IMPLICIT);
        } catch (Exception e) {
        }
    }

    /**
     * 显示软键盘2
     *
     * @param act
     */
    public static void showSoftKeyBoard(Context act, View v) {
        try {
            if (v != null) {
                v.requestFocus();
            }
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
        } catch (Exception e) {
        }
    }


    /**
     * 复制到粘贴板
     *
     * @param lable
     * @param charSequence
     */
    public static void copyTextToClipboard(@Nullable String lable, @NonNull CharSequence charSequence) {
        ClipboardManager cmb = (ClipboardManager) XXF.getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cmb == null) {
            return;
        }
        //将内容放入粘贴管理器,在别的地方长按选择"粘贴"即可
        cmb.setPrimaryClip(ClipData.newPlainText(lable, charSequence));
    }

    /**
     * 复制到粘贴板
     *
     * @param charSequence
     */
    public static void copyTextToClipboard(@NonNull CharSequence charSequence) {
        ClipboardManager cmb = (ClipboardManager) XXF.getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cmb == null) {
            return;
        }
        //将内容放入粘贴管理器,在别的地方长按选择"粘贴"即可
        cmb.setPrimaryClip(ClipData.newPlainText("text", charSequence));
    }

    /**
     * 获取粘贴板内容
     *
     * @return
     */
    public static CharSequence getTextFromClipboard() {
        ClipboardManager cmb = (ClipboardManager) XXF.getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cmb != null) {
            if (cmb.hasPrimaryClip()) {
                if (cmb.getPrimaryClip().getItemCount() > 0) {
                    return cmb.getPrimaryClip().getItemAt(0).getText();
                }
            }
        }
        return null;
    }

    /**
     * 监听软键盘是否点击了搜索／下一步／发送／完成／回车按钮
     * （在TextView.OnEditorActionListener的 onEditorAction(TextView v, int actionId, KeyEvent event)方法监听）
     *
     * @param actionId
     * @param event
     * @return
     */
    public static boolean isSearchClick(int actionId, KeyEvent event) {
        return actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_DONE
                || (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction());
    }


    private static Uri queryMediaImageUri(File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = XXF.getApplication().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "_data=? ", new String[]{filePath}, (String) null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex("_id"));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else if (imageFile.exists()) {
            ContentValues values = new ContentValues();
            values.put("_data", filePath);
            return XXF.getApplication().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            return null;
        }
    }

    public static class CropIntentBuilder {
        private Intent mCropIntent = new Intent("com.android.camera.action.CROP");

        public CropIntentBuilder() {
            //默认值 不能修改!!!!!
            this.mCropIntent.putExtra("crop", true);
            this.mCropIntent.putExtra("outputX", 320);
            this.mCropIntent.putExtra("outputY", 320);
            this.mCropIntent.putExtra("scale", true);
            this.mCropIntent.putExtra("aspectX", 1);
            this.mCropIntent.putExtra("aspectY", 1);
            this.mCropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            this.mCropIntent.putExtra("noFaceDetection", true);
            this.mCropIntent.putExtra("scale", true);
            this.mCropIntent.putExtra("scaleUpIfNeeded", true);
        }

        public CropIntentBuilder inputImgUri(Uri inImgUri) {
            this.mCropIntent.setDataAndType(inImgUri, "image/*");
            return this;
        }

        public CropIntentBuilder inputImgFile(File inImgFile) {
            Uri inImgUri = queryMediaImageUri(inImgFile);
            this.mCropIntent.setDataAndType(inImgUri, "image/*");
            return this;
        }

        public CropIntentBuilder aspectXY(int x, int y) {
            this.mCropIntent.putExtra("aspectX", x);
            this.mCropIntent.putExtra("aspectY", y);
            return this;
        }

        public CropIntentBuilder outputXY(int x, int y) {
            this.mCropIntent.putExtra("outputX", x);
            this.mCropIntent.putExtra("outputY", y);
            return this;
        }

        public CropIntentBuilder outputBitmap() {
            this.mCropIntent.putExtra("return-data", true);
            return this;
        }

        public CropIntentBuilder outputUri(Uri outputImgUri) {
            this.mCropIntent.putExtra("return-data", false);
            this.mCropIntent.putExtra("output", outputImgUri);
            return this;
        }

        public CropIntentBuilder outputFile(File outputImgFile) {
            this.mCropIntent.putExtra("return-data", false);
            this.mCropIntent.putExtra("output", queryMediaImageUri(outputImgFile));
            return this;
        }

        public Intent build() {
            return this.mCropIntent;
        }
    }

    /**
     * 分享文本
     *
     * @param context
     * @param text    分享文本
     * @return
     */
    public static Observable<ActivityResult> shareText(Context context, String text) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        try {
            String scheme = Uri.parse(text).getScheme();
            if (TextUtils.equals(scheme, "http") || TextUtils.equals(scheme, "https")) {
                sendIntent.setType("text/html");
            } else {
                sendIntent.setType("text/plain");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            sendIntent.setType("text/plain");
        }
        Intent chooser = Intent.createChooser(sendIntent, "share text");
        if (context instanceof LifecycleOwner) {
            return XXF.startActivityForResult((LifecycleOwner) context, chooser, REQUEST_CODE_SHARE);
        } else {
            return Observable
                    .fromCallable(new Callable<ActivityResult>() {
                        @Override
                        public ActivityResult call() throws Exception {
                            context.startActivity(chooser);
                            return new ActivityResult(REQUEST_CODE_SHARE, Activity.RESULT_OK, new Intent());
                        }
                    });
        }
    }

    /**
     * 分享文件
     *
     * @param context
     * @param filePath  文件
     * @param authority
     * @return
     */
    public static Observable<ActivityResult> shareFile(Context context, String filePath, String authority) {
        File file = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_SEND);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, authority, file);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        // 授予目录临时共享权限
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        String fileType = null;
        if (uri != null) {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                fileType = contentResolver.getType(uri);
            }
        }
        if (TextUtils.isEmpty(filePath)) {
            fileType = "*/*";
        }
        intent.setDataAndType(uri, fileType);
        Intent chooser = Intent.createChooser(intent, file.getName());
        if (context instanceof LifecycleOwner) {
            return XXF.startActivityForResult((LifecycleOwner) context, chooser, REQUEST_CODE_SHARE);
        } else {
            return Observable
                    .fromCallable(new Callable<ActivityResult>() {
                        @Override
                        public ActivityResult call() throws Exception {
                            context.startActivity(chooser);
                            return new ActivityResult(REQUEST_CODE_SHARE, Activity.RESULT_OK, new Intent());
                        }
                    });
        }
    }
}
