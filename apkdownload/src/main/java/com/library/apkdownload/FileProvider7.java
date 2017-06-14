package com.library.apkdownload;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.util.List;

/**
 * 兼容7.0的文件提供者[可供外部调用，所以单独成类]
 * 参考：https://github.com/hongyangAndroid/FitAndroid7
 * Created by zmq on 2017/6/9.
 */

public class FileProvider7 {
    /**
     * 获取文件的uri
     *
     * @param context 上下文
     * @param file    文件
     * @return uri
     */
    public static Uri getFileUri(Context context, File file) {
        Uri fileUri;
        if (Build.VERSION.SDK_INT >= 24) {
            fileUri = getFileUriOn24(context, file);
        } else {
            fileUri = Uri.fromFile(file);
        }
        return fileUri;
    }

    /**
     * 7.0系统及以上获取文件的uri
     *
     * @param context 上下文
     * @param file    文件
     * @return uri
     */
    private static Uri getFileUriOn24(Context context, File file) {
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider",
                file);
    }

    /**
     * 设置Intent的数据以及类型
     *
     * @param context   上下文
     * @param intent    意图对象
     * @param type      类型
     * @param file      文件
     * @param writeAble 是否可写
     */
    public static void setDataAndType(Context context,
                                      Intent intent,
                                      String type,
                                      File file,
                                      boolean writeAble) {
        if (Build.VERSION.SDK_INT >= 24) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (writeAble) {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        }
        intent.setDataAndType(getFileUri(context, file), type);
    }

    /**
     * 设置Intent数据
     *
     * @param context   上下文
     * @param intent    意图对象
     * @param file      文件
     * @param writeAble 是否可写
     */
    public static void setData(Context context,
                               Intent intent,
                               File file,
                               boolean writeAble) {
        intent.setData(getFileUri(context, file));
        if (Build.VERSION.SDK_INT >= 24) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (writeAble) {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        }
    }

    /**
     * 授权
     *
     * @param context   上下文
     * @param intent    意图对象
     * @param uri       文件uri
     * @param writeAble 是否可写
     */
    public static void grantPermissions(Context context, Intent intent, Uri uri, boolean writeAble) {

        int flag = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (writeAble) {
            flag |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        }
        intent.addFlags(flag);
        List<ResolveInfo> resInfoList = context.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, flag);
        }
    }


}
