package com.shishi.shishiface.faceserver

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import android.util.Log

class FlashLight(val context: Context) {

    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun changeFlashLight(openOrClose: Boolean) {

        fun oldCamera() {
            val camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
            val parameters = camera.parameters
            if (openOrClose) {
                //打开闪光灯
                parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH//开启
                camera.parameters = parameters
            } else {
                //关闭闪光灯
                parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF//关闭
                camera.parameters = parameters
            }
        }

        //判断API是否大于24（安卓7.0系统对应的API）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                //获取CameraManager
                val mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                //获取当前手机所有摄像头设备ID
                val id = "0"
                val c = mCameraManager.getCameraCharacteristics(id)
                //查询该摄像头组件是否包含闪光灯
                val flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val lensFacing = c.get(CameraCharacteristics.LENS_FACING)
                if (flashAvailable != null && flashAvailable
                    && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK
                ) {
                    //打开或关闭手电筒
                    mCameraManager.setTorchMode(id, openOrClose)
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                oldCamera()
            }
        } else oldCamera()
    }

    fun flash(times: Int, interval: Long) {
        Thread {
            for (i in 1..times) {
                changeFlashLight(true)
                Thread.sleep(interval)
                changeFlashLight(false)
                Thread.sleep(interval)
            }
            changeFlashLight(false)
        }.start()
    }

    fun light(time: Long) {
        Thread {
            changeFlashLight(true)
            Thread.sleep(time)
            changeFlashLight(false)
        }.start()
    }

}