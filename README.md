# ApkDownload主要功能：利用系统DownloadManager封装实现APK的下载、通知栏提示、安装，并适配至7.0系统。

![首页](apkdownload.gif)

使用方法：

1、在 build.gradle dependencies添加
compile 'com.zmq.apkdownload:apkdownload:1.0.0'

2、API调用示例:
ApkMgr.getInstance(this).download(url, new ApkMgr.DownLoadListener() {

            @Override
            public void onDownloadStart() {
                Log.e("download", "onDownloadStart()下载开始");
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
