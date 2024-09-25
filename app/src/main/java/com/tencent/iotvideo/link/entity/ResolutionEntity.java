package com.tencent.iotvideo.link.entity;

import androidx.annotation.NonNull;

public class ResolutionEntity {
    public static String TAG = ResolutionEntity.class.getSimpleName();
    /**
     * 分辨率宽
     */
    private int width;
    /**
     * 分辨率高
     */
    private int height;
    /**
     * 分辨率简称 如 360p, 540p, 720p, 1080p
     */
    private String simpleName = "";

    public ResolutionEntity(){

    }

    public ResolutionEntity(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public ResolutionEntity(int width, int height, String simpleName) {
        this.width = width;
        this.height = height;
        this.simpleName = simpleName;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    @NonNull
    @Override
    public String toString() {
        return simpleName;
    }
}
