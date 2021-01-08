package com.yks.download;

/**
 * @Description: 下载文件回调接口 成功/失败，进度条
 * @Author: Yu ki-r
 * @CreateDate: 2021/1/7 16:55
 */
public interface DownloadCallBack {

    void onSuccess(DownloadInfo downloadInfo);

    void onFailure(String message);

    void onProgress(long totalSize, long downloadSize);
}
