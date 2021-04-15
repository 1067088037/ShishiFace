package com.shishi.shishiface.model;

import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;

public class FacePreviewInfo {

    private FaceInfo faceInfo;
    private LivenessInfo livenessInfo;
    private GenderInfo genderInfo;

    public FacePreviewInfo(FaceInfo faceInfo, LivenessInfo livenessInfo, GenderInfo genderInfo /*,int trackId*/) {
        this.faceInfo = faceInfo;
        this.livenessInfo = livenessInfo;
        this.genderInfo = genderInfo;
    }

    public FaceInfo getFaceInfo() {
        return faceInfo;
    }

    public void setFaceInfo(FaceInfo faceInfo) {
        this.faceInfo = faceInfo;
    }

    public LivenessInfo getLivenessInfo() {
        return livenessInfo;
    }

    public void setLivenessInfo(LivenessInfo livenessInfo) {
        this.livenessInfo = livenessInfo;
    }

    public void setGenderInfo(GenderInfo genderInfo) {this.genderInfo = genderInfo; }

    public GenderInfo getGenderInfo() {return  genderInfo; }

}
