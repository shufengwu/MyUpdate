package com.example.wushufeng.myupdate.mylibrary;

/**
 * Created by wushufeng on 2016/12/19.
 */

public class UpdateBean {

    /**
     * version : 2.0
     * url : https://github.com/shufengwu/update_server/raw/master/app-debug.apk
     * description : 检测到最新版本，请及时更新！
     */

    private String version;
    private String url;
    private String description;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
