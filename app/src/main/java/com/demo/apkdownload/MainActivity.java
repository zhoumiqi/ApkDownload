package com.demo.apkdownload;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.library.apkdownload.ApkMgr;

public class MainActivity extends AppCompatActivity {
    private static final String URL = "http://dianhua.118114.cn/bstapp/besttone_9000_3.apk";
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dialog = new ProgressDialog(this);
    }

    public void downloadApk(View view) {
        ApkMgr.getInstance(this).download(URL, new ApkMgr.DownLoadListener() {

            @Override
            public void onDownloadStart() {
                dialog.setMessage("下载开始");
                dialog.show();
            }

            @Override
            public void onDownloading(ApkMgr.DownloadModel model) {
                if (model == null) {
                    return;
                }
                int status = model.getDownloadStatus();
                if (DownloadManager.STATUS_RUNNING == status) {
                    Log.e("download", "正在下载");
                } else if (DownloadManager.STATUS_SUCCESSFUL == status) {
                    Log.e("download", "下载完成");
                } else if (DownloadManager.STATUS_PAUSED == status) {
                    Log.e("download", "下载暂停");
                } else if (DownloadManager.STATUS_FAILED == status) {
                    Log.e("download", "下载失败");
                }
                int percent = (int) (model.getCurSize() * 1.0f / model.getTotalSize() * 100);
                Log.e("download", String.format("id=%d,curSize=%d,totalSize=%d,percent=%s", model.getDownloadId(), model.getCurSize(), model.getTotalSize(), percent + "%"));
                dialog.setMax(model.getTotalSize());
                dialog.setProgress(model.getCurSize());
                dialog.setMessage("下载中：已完成 " + percent + "%");
            }

            @Override
            public void onDownloadComplete() {
                Log.e("download", "onDownloadComplete()下载完成");
                dialog.setMessage("下载完成");
                dialog.dismiss();
            }
        });
    }
}
