# ApkDownload主要功能：利用系统DownloadManager封装实现APK的下载、通知栏提示、安装，并适配至7.0系统。
使用方法：

1、在 build.gradle dependencies添加
compile 'com.zmq.apkdownload:apkdownload:0.0.2'

2、API调用示例:
ApkMgr.getInstance(this).download(url, new ApkMgr.DownLoadListener() {

            @Override
            public void onDownloadStart() {
                Log.e("download", "onDownloadStart() 下载开始");
            }

            @Override
            public void onDownloading(ApkMgr.DownloadModel model) {
                Log.e("download", "onDownloading() 下载中");
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
                Log.e("download", String.format("下载id=%d,当前下载的文件大小=%d,文件总大小=%d,下载百分比=%s", model.getDownloadId(),   model.getCurSize(), model.getTotalSize(), percent + "%"));
            }

            @Override
            public void onDownloadComplete() {
                Log.e("download", "onDownloadComplete()下载完成");
            }
        });
