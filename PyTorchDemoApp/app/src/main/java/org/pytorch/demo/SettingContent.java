package org.pytorch.demo;

public class SettingContent {
    String rtmp_addr;
    String server_addr;
    Boolean save_file;
    String decode_method;
    String resolution;
    int fps;
    String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRtmp_addr() {
        return rtmp_addr;
    }

    public void setRtmp_addr(String rtmp_addr) {
        this.rtmp_addr = rtmp_addr;
    }

    public String getServer_addr() {
        return server_addr;
    }

    public void setServer_addr(String server_addr) {
        this.server_addr = server_addr;
    }

    public Boolean getSave_file() {
        return save_file;
    }

    public void setSave_file(Boolean save_file) {
        this.save_file = save_file;
    }

    public String getDecode_method() {
        return decode_method;
    }

    public void setDecode_method(String decode_method) {
        this.decode_method = decode_method;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public SettingContent(String rtmp_addr, String server_addr, Boolean save_file, String decode_method, String resolution, int fps) {
        this.rtmp_addr = rtmp_addr;
        this.server_addr = server_addr;
        this.save_file = save_file;
        this.decode_method = decode_method;
        this.resolution = resolution;
        this.fps = fps;
    }
}
