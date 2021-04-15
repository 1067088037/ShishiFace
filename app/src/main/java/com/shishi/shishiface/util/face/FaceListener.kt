package com.shishi.shishiface.util.face

import com.arcsoft.face.FaceFeature
import com.arcsoft.face.GenderInfo
import java.lang.reflect.Array

interface FaceListener {

    /**
     * 当出现异常时执行
     *
     * @param e 异常信息
     */
    fun onFail(e: Exception)


    /**
     * 请求人脸特征后的回调
     *
     * @param faceFeature  人脸特征数据
     * @param msg  向请求者传回信息 { i=0 人脸宽度; i=1 人脸高度}
     * @param startTime 开始时的时间
     * */
    fun onFaceFeatureInfoGet(faceFeature: FaceFeature?, msg: ArrayList<Int>?, startTime: Long = -1)

}
