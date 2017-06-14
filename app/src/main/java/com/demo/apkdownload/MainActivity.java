package com.demo.apkdownload;

import android.app.DownloadManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.library.apkdownload.ApkMgr;


public class MainActivity extends AppCompatActivity {
    private static final String URL = "http://dianhua.118114.cn/bstapp/besttone_9000_3.apk";
    private TextView tvInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvInfo= (TextView) findViewById(R.id.tv_info);
    }

    public void downloadApk(View view) {
        ApkMgr.getInstance(this).download(URL, new ApkMgr.DownLoadListener() {

            @Override
            public void onDownloadStart() {
                tvInfo.setText("下载开始");
            }

            @Override
            public void onDownloading(ApkMgr.DownloadModel model) {
                if (model == null) {
                    return;
                }
                int status = model.getDownloadStatus();
                switch (status) {
                    case DownloadManager.STATUS_RUNNING:
                        Log.e("download", "正在下载");
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        Log.e("download", "下载完成");
                        break;
                    case DownloadManager.STATUS_PAUSED:
                        Log.e("download", "下载暂停");
                        break;
                    case DownloadManager.STATUS_FAILED:
                        Log.e("download", "下载失败");
                        break;
                }
                int percent = (int) (model.getCurSize() * 1.0f / model.getTotalSize() * 100);
                Log.e("download", String.format("id=%d,curSize=%d,totalSize=%d,percent=%s", model.getDownloadId(), model.getCurSize(), model.getTotalSize(), percent + "%"));
                tvInfo.setText("下载中：已完成 " + percent + "%");
            }

            @Override
            public void onDownloadComplete() {
                Log.e("download", "onDownloadComplete()下载完成");
                tvInfo.setText("下载完成");
            }
        });
    }
}
