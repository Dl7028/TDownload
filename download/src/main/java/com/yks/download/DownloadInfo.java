package com.yks.download;

/**
 * @Description: 下载实体类
 * @Author: Yu ki-r
 * @CreateDate: 2021/1/7 16:57
 */
public class DownloadInfo {

    public static final long TOTAL_ERROR = -1;  //进度条获取失败
    private String url; //下载url
    private long totalSize;//下载大小
    private long progress; //下载进度
    private String fileName; //下载文件名
    private String savePath;//下载的保存路径

    public DownloadInfo(String url){
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    @Override
    public String toString() {
        return "DownloadInfo{}";
    }
}
