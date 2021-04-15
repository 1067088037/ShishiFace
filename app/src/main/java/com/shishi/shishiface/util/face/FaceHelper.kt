package com.shishi.shishiface.util.face

import android.graphics.Rect
import android.hardware.Camera
import android.util.Log
import com.arcsoft.face.*
import com.shishi.shishiface.Config

import com.shishi.shishiface.model.FacePreviewInfo
import com.shishi.shishiface.util.ConfigUtil
import com.shishi.shishiface.util.TrackUtil

import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class FaceHelper private constructor(builder: Builder) {

    private val faceEngine: FaceEngine?
    private val previewSize: Camera.Size?

    /**
     * fr 线程数，建议和ft初始化时的maxFaceNum相同
     */
    private var frThreadNum = 1

    private var faceInfoList: MutableList<FaceInfo>? = ArrayList()
    private val livenessInfoList = ArrayList<LivenessInfo>()
    private val genderInfoList = ArrayList<GenderInfo>()
    private val executor = Executors.newSingleThreadExecutor()
    private var frThreadRunning = false
    private var faceListener: FaceListener? = null
    private var faceRecognizeRunnables: LinkedBlockingQueue<FaceRecognizeRunnable>? = null

    //trackId相关
    /**
     * 获取当前的最大trackID,可用于退出时保存
     *
     * @return 当前trackId
     */
//    private var currentTrackId = 0
    private val formerTrackIdList = ArrayList<Int>()
//    private val currentTrackIdList = ArrayList<Int>()
    private val formerFaceRectList = ArrayList<Rect>()
    private val facePreviewInfoList = ArrayList<FacePreviewInfo>()
    private var nameMap: ConcurrentHashMap<Int, String>? = ConcurrentHashMap()

    init {
        faceEngine = builder.faceEngine
        faceListener = builder.faceListener
//        currentTrackId = builder.currentTrackId
        previewSize = builder.previewSize
        if (builder.frThreadNum > 0) {
            frThreadNum = builder.frThreadNum
            faceRecognizeRunnables = LinkedBlockingQueue(frThreadNum)
        } else {
            Log.e(TAG, "frThread num must > 0,now using default value:$frThreadNum")
        }
        if (previewSize == null) {
            throw RuntimeException("previewSize must be specified!")
        }
    }

    /**
     * 请求获取人脸特征数据，需要传入FR的参数，以下参数同 AFR_FSDKEngine.AFR_FSDK_ExtractFRFeature
     *
     * @param nv21     NV21格式的图像数据
     * @param faceInfo 人脸信息
     * @param width    图像宽度
     * @param height   图像高度
     * @param format   图像格式
//     * @param trackId  请求人脸特征的唯一请求码，一般使用trackId
     */
    fun requestFaceFeature(nv21: ByteArray, faceInfo: FaceInfo, width: Int, height: Int, format: Int, facePreviewInfo: List<FacePreviewInfo>) {
        if (faceListener != null) {
            if (faceEngine != null && faceRecognizeRunnables != null && faceRecognizeRunnables!!.size < frThreadNum && !frThreadRunning) {
                faceRecognizeRunnables!!.add(FaceRecognizeRunnable(nv21, faceInfo, width, height, format, facePreviewInfo))
                executor.execute(faceRecognizeRunnables!!.poll())
            } else {
//                faceListener!!.onFaceFeatureInfoGet(null, trackId)
            }
        }
    }

    fun release() {
        if (!executor.isShutdown) {
            executor.shutdown()
        }
        if (faceInfoList != null) {
            faceInfoList!!.clear()
        }
        if (faceRecognizeRunnables != null) {
            faceRecognizeRunnables!!.clear()
        }
        if (nameMap != null) {
            nameMap!!.clear()
        }
        faceRecognizeRunnables = null
        nameMap = null
        faceListener = null
        faceInfoList = null
    }

    fun onPreviewFrame(nv21: ByteArray): List<FacePreviewInfo> {
        if (faceListener != null) {
            if (faceEngine != null) {
                faceInfoList!!.clear()
                var code = faceEngine.detectFaces(
                    nv21,
                    previewSize!!.width,
                    previewSize.height,
                    FaceEngine.CP_PAF_NV21,
                    faceInfoList
                )
                if (code != ErrorInfo.MOK) {
                    faceListener!!.onFail(Exception("ft failed,code is $code"))
                }

                TrackUtil.keepMaxFace(faceInfoList)
                code = faceEngine.process(
                    nv21,
                    previewSize.width,
                    previewSize.height,
                    FaceEngine.CP_PAF_NV21,
                    faceInfoList,
                    (FaceEngine.ASF_LIVENESS or FaceEngine.ASF_GENDER)
                )
                if (code != ErrorInfo.MOK) {
                    faceListener!!.onFail(Exception("process failed,code is $code"))
                }
                code = faceEngine.getLiveness(livenessInfoList)
                if (code != ErrorInfo.MOK) {
                    faceListener!!.onFail(Exception("getLiveness failed,code is $code"))
                }
                code = faceEngine.getGender(genderInfoList)
                if (code != ErrorInfo.MOK) {
                    faceListener!!.onFail(Exception("getGender failed,code is $code"))
                }
            }

            facePreviewInfoList.clear()
            if (livenessInfoList.size == faceInfoList!!.size) {
                for (i in faceInfoList!!.indices) {
                    facePreviewInfoList.add(
                        FacePreviewInfo(
                            faceInfoList!![i],
                            livenessInfoList[i],
                            genderInfoList[i]
                        )
                    )
                }
            }
            return facePreviewInfoList
        } else {
            facePreviewInfoList.clear()
            return facePreviewInfoList
        }
    }

    /**
     * 人脸解析的线程
     */
    inner class FaceRecognizeRunnable constructor(
        private var nv21Data: ByteArray?,
        faceInfo: FaceInfo,
        private val width: Int,
        private val height: Int,
        private val format: Int,
        private val facePreviewInfo: List<FacePreviewInfo>
    ) : Runnable {
        private val faceInfo: FaceInfo = FaceInfo(faceInfo)

        override fun run() {
            frThreadRunning = true
            if (faceListener != null && nv21Data != null) {
                if (faceEngine != null) {
                    val faceFeature = FaceFeature()
                    val frStartTime = System.currentTimeMillis()
                    val frCode: Int
                    synchronized(this@FaceHelper) {
                        frCode = faceEngine.extractFaceFeature(nv21Data, width, height, format, faceInfo, faceFeature)
                    }
                    if (frCode == ErrorInfo.MOK) {
                        val msg = ArrayList<Int>()
                        msg.add(faceInfo.rect.width())
                        msg.add(faceInfo.rect.height())
                        msg.add(
                            if (facePreviewInfo.isNotEmpty())//鬼知道Android有什么bug
                                try {
                                    facePreviewInfo[0].livenessInfo.liveness
                                } catch (e: IndexOutOfBoundsException) {
                                    LivenessInfo.UNKNOWN
                                }
                            else LivenessInfo.UNKNOWN
                        )
                        faceListener?.onFaceFeatureInfoGet(faceFeature, msg, frStartTime)
                    } else {
                        faceListener?.onFaceFeatureInfoGet(null, null, frStartTime)
                        faceListener?.onFail(Exception("fr failed errorCode is $frCode"))
                    }
                }
                if (faceRecognizeRunnables != null && faceRecognizeRunnables!!.size > 0) {
                    executor.execute(faceRecognizeRunnables!!.poll())
                }
            }
            nv21Data = null
            frThreadRunning = false
        }
    }

    class Builder {
        var faceEngine: FaceEngine? = null
        var previewSize: Camera.Size? = null
        var faceListener: FaceListener? = null
        var frThreadNum: Int = 0
        var currentTrackId: Int = 0

        fun faceEngine(`val`: FaceEngine): Builder {
            faceEngine = `val`
            return this
        }

        fun previewSize(`val`: Camera.Size): Builder {
            previewSize = `val`
            return this
        }

        fun faceListener(`val`: FaceListener): Builder {
            faceListener = `val`
            return this
        }

        fun frThreadNum(`val`: Int): Builder {
            frThreadNum = `val`
            return this
        }

        fun currentTrackId(`val`: Int): Builder {
            currentTrackId = `val`
            return this
        }

        fun build(): FaceHelper {
            return FaceHelper(this)
        }
    }

    companion object {
        private const val TAG = "FaceHelper"
        private const val SIMILARITY_RECT = 0.3f
    }

}