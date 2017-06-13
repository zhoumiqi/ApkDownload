package com.library.apkdownload;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * APK管理器
 * 实现下载、安装功能
 * <p>参考：</p>
 * <p>
 * http://blog.csdn.net/cfy137000/article/details/70257912
 * http://blog.csdn.net/lmj623565791/article/details/72859156
 * </p>
 * Created by zmq on 2017/6/9.
 */

public class ApkMgr {
    private static final String MIME_TYPE = "application/vnd.android.package-archive";
    private static ApkMgr instance;
    private ExecutorService mThreadPool;
    private static final int QUERY_COMPLETE = 0;//查询完成
    private DownloadManager mDownloadManager;
    private DownloadChangeObserver mObserver;
    private LongSparseArray<WeakReference<DownLoadListener>> mListeners;
    private LongSparseArray<File> mApkPaths;
    private WeakReference<Context> mContext;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case QUERY_COMPLETE://数据库查询完成
                    List<DownloadModel> list = (List<DownloadModel>) msg.obj;
                    for (DownloadModel model : list) {
                        long downloadId = model.getDownloadId();
                        WeakReference<DownLoadListener> ref = mListeners.get(downloadId);
                        if (ref == null) {
                            return false;
                        }
                        DownLoadListener listener = ref.get();
                        if (listener != null) {
                            listener.onDownloading(model);
                        }
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    private ApkMgr(Context context) {
        mContext = new WeakReference<>(context.getApplicationContext());
        mDownloadManager = (DownloadManager) mContext.get()
                .getSystemService(Context.DOWNLOAD_SERVICE);
        mListeners = new LongSparseArray<>();
        mApkPaths = new LongSparseArray<>();
    }

    public static ApkMgr getInstance(Context context) {
        if (instance == null) {
            synchronized (ApkMgr.class) {
                if (instance == null) {
                    instance = new ApkMgr(context);
                }
            }
        }
        return instance;
    }

    /**
     * 获取线程池
     *
     * @return 线程池对象
     */
    private ExecutorService getThreadPool() {
        if (mThreadPool == null) {
            synchronized (ExecutorService.class) {
                if (mThreadPool == null) {
                    mThreadPool = Executors.newCachedThreadPool();
                }
            }
        }
        return mThreadPool;
    }

    /**
     * 线程执行器
     *
     * @param runnable 线程
     */
    private static void execute(Runnable runnable) {
        instance.getThreadPool().execute(runnable);
    }

    /**
     * 取消正在执行的线程任务
     */
    private synchronized void shutDownNow() {
        if (mThreadPool != null) {
            try {
                mThreadPool.shutdownNow();
                mThreadPool = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 取消正在执行的线程任务
     */
    public static void onCancel() {
        try {
            instance.shutDownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载
     * <p>以当前系统时间的long值为apk名字</p>
     *
     * @param url 需要下载的apk地址
     * @return 下载id
     */
    public long download(String url) {
        try {
            return executeDownload(url, null, null);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return -1;
    }

    /**
     * 下载
     * <p>以当前系统时间的long值为apk名字</p>
     *
     * @param url      需要下载的apk地址
     * @param listener 下载状态的监听器
     * @return 下载id
     */
    public long download(String url, DownLoadListener listener) {
        try {
            return executeDownload(url, null, listener);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return -1;
    }

    /**
     * 下载
     *
     * @param url     需要下载的apk地址
     * @param apkName apk名字[以.apk结尾,如果传null则以当前系统时间的long值为名字]
     * @return 下载id
     */
    public long download(String url, String apkName) {
        try {
            return executeDownload(url, apkName, null);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return -1;
    }

    /**
     * 下载
     *
     * @param url      需要下载的apk地址
     * @param apkName  apk名字[以.apk结尾,如果传null则以当前系统时间的long值为名字]
     * @param listener 下载状态的监听器
     * @return 下载id
     */
    public long download(String url, String apkName, DownLoadListener listener) {
        try {
            return executeDownload(url, apkName, listener);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return -1;
    }

    /**
     * 执行下载
     */
    private long executeDownload(String url, String apkName, DownLoadListener listener) throws Throwable {
        if (mDownloadManager == null) {
            throw new Throwable("please init first");
        }
        if (mContext.get() == null) {
            throw new Throwable("weakReference context is null");
        }
        if (TextUtils.isEmpty(apkName) || !apkName.endsWith(".apk")) {
            apkName = System.currentTimeMillis() + ".apk";
        }
        if (listener != null) {
            listener.onDownloadStart();
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.allowScanningByMediaScanner();
        //使用setDestinationInExternalPublicDir这个方法，在部分机型上会报错，例如华为Mate8
        //下面代码等价于request.setDestinationInExternalFilesDir(context.getApplicationContext(), Environment.DIRECTORY_DOWNLOADS, apkName);
        File file = new File(mContext.get().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkName);
        request.setDestinationUri(Uri.fromFile(file));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(true);
        request.setTitle(System.currentTimeMillis()+"");
        request.setMimeType(MIME_TYPE);
        long downloadId = mDownloadManager.enqueue(request);
        mApkPaths.put(downloadId, file);
        if (listener != null) {
            addListener(listener, downloadId);
            if (mObserver == null) {
                mObserver = new DownloadChangeObserver(mHandler);
                mContext.get().getContentResolver().registerContentObserver(
                        Uri.parse("content://downloads/my_downloads"), true, mObserver);
            }
            mObserver.addId(downloadId);
        }
        return downloadId;
    }

    /**
     * 添加监听器
     *
     * @param listener   监听器
     * @param downloadId 下载id
     */
    private void addListener(DownLoadListener listener, long downloadId) {
        mListeners.put(downloadId, new WeakReference<>(listener));
    }

    /**
     * 移除监听器
     *
     * @param downloadId 下载id
     */
    private void removeListener(long downloadId) {
        mListeners.remove(downloadId);
    }


    /**
     * 查询下载进度
     *
     * @param downloadId 要获取下载的id
     * @return 进度百分比值 [取值范围0-100]
     */
    public int getDownloadProgress(long downloadId) {
        // TODO: 2017/6/9 参照源码如何将下载进度告诉通知栏显示进度的，而不用反复查询数据库呢？
        //查询进度
        DownloadManager.Query query = new DownloadManager.Query()
                .setFilterById(downloadId);
        Cursor cursor = null;
        int progress = 0;
        try {
            cursor = mDownloadManager.query(query);//获得游标
            if (cursor != null && cursor.moveToFirst()) {
                //当前的下载量
                int downloadSoFar = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                //文件总大小
                int totalBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                progress = (int) (downloadSoFar * 1.0f / totalBytes * 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return progress;
    }


    /**
     * 安装APK
     *
     * @param context 上下文
     */
    private static void installApk(Context context, long downloadId) {
        File file = instance.mApkPaths.get(downloadId);
        if (!file.exists()) {
            System.out.println("apk not exists");
            return;
        }
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            FileProvider7.setDataAndType(context, install, MIME_TYPE, file, true);
            context.startActivity(install);
        } catch (Exception e) {
            //android.content.ActivityNotFoundException: No Activity found to handle Intent
            //上面这个错误是系统找不到可以处理安装APK这个Intent的Activity,也就是说手机没有可以处理安装操作的应用程序
            System.out.println("install occur error : " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * 通知下载完成
     *
     * @param downloadId 下载id
     */
    private static void downloadComplete(long downloadId) {
        WeakReference<DownLoadListener> ref = instance.mListeners.get(downloadId);
        if (ref != null) {
            DownLoadListener listener = ref.get();
            if (listener != null) {
                listener.onDownloadComplete();//通过观察数据库变动不一定能获取到下载完成的状态，所以通过广播来通知
            }
        }
        instance.removeListener(downloadId);
    }

    /**
     * 下载状态的观察者
     */
    private class DownloadChangeObserver extends ContentObserver {

        private Handler handler;
        private List<Long> ids;

        DownloadChangeObserver(Handler handler) {
            super(handler);
            this.handler = handler;
            ids = new ArrayList<>();
        }

        void addId(long id) {
            ids.add(id);
        }

        void removeId(long id) {
            ids.remove(id);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            queryDownloadStatus(handler, ids);
        }
    }

    /**
     * 查询下载状态
     *
     * @param handler 消息句柄
     * @param ids     下载id
     */
    private void queryDownloadStatus(final Handler handler, List<Long> ids) {
        int size = ids.size();
        final long[] idArr = new long[size];
        for (int i = 0; i < size; i++) {
            idArr[i] = ids.get(i);
        }
        execute(new Runnable() {
            @Override
            public void run() {
                List<DownloadModel> list = getDownloadModelList(idArr);
                if (list != null && list.size() > 0) {
                    handler.sendMessage(handler.obtainMessage(QUERY_COMPLETE, list));
                }
            }
        });
    }

    /**
     * 获取正在下载的APK信息
     *
     * @param ids 下载id
     * @return apk信息
     */
    private List<DownloadModel> getDownloadModelList(long... ids) {
        DownloadManager.Query query = new DownloadManager.Query()
                .setFilterById(ids);
        Cursor cursor = null;
        List<DownloadModel> list = new ArrayList<>();
        DownloadModel model;
        try {
            cursor = mDownloadManager.query(query);
            while (cursor != null && cursor.moveToNext()) {
                model = new DownloadModel();
                int curSize = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int totalSize = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                long downloadId = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                model.setDownloadId(downloadId);
                model.setCurSize(curSize);
                model.setTotalSize(totalSize);
                model.setDownloadStatus(status);
                model.setApkPath(mApkPaths.get(downloadId).getAbsolutePath());
                list.add(model);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return list;
    }

    /**
     * 下载实体对象
     */
    public class DownloadModel implements Serializable {
        /**
         * 下载id
         */
        private long downloadId;
        /**
         * 当前下载大小，单位byte
         */
        private int curSize;
        /**
         * 总共下载大小，单位byte
         */
        private int totalSize;
        /**
         * 当前的下载状态
         */
        private int downloadStatus;
        /**
         * 下载的apk绝对路径
         */
        private String apkPath;

        public String getApkPath() {
            return apkPath;
        }

        void setApkPath(String apkPath) {
            this.apkPath = apkPath;
        }

        public long getDownloadId() {
            return downloadId;
        }

        void setDownloadId(long downloadId) {
            this.downloadId = downloadId;
        }

        public int getCurSize() {
            return curSize;
        }

        void setCurSize(int curSize) {
            this.curSize = curSize;
        }

        public int getTotalSize() {
            return totalSize;
        }

        void setTotalSize(int totalSize) {
            this.totalSize = totalSize;
        }

        public int getDownloadStatus() {
            return downloadStatus;
        }

        void setDownloadStatus(int downloadStatus) {
            this.downloadStatus = downloadStatus;
        }
    }

    /**
     * 下载监听器
     * Created by zmq on 2017/6/9.
     */
    public interface DownLoadListener {
        /**
         * 开始下载
         */
        void onDownloadStart();

        /**
         * 正在下载
         *
         * @param model 下载的APK信息
         */
        void onDownloading(DownloadModel model);

        /**
         * 完成下载
         */
        void onDownloadComplete();
    }

    /**
     * APK下载完成的广播接收器
     * <p>
     * 注册内部类广播注意事项
     * 1、清单文件注册时需要在内部类所在的类与内部类之间加上$符号:
     * 2、内部类在声明时一定要写成静态内部类（class关键字前加上static）。否则会抛出类似这样的异常：
     * Unable to instantiate receiver …… has no zero argument constructor
     * </p>
     */
    public static class ApkDownloadReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                return;
            }
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == -1) {
                    return;
                }
                //通知下载完成并安装APK
                downloadComplete(downloadId);
                installApk(context, downloadId);
            }
        }
    }
}
