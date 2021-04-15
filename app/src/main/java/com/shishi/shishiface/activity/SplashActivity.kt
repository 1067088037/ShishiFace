package com.shishi.shishiface.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shishi.shishiface.Config
import com.shishi.shishiface.R
import kotlin.system.exitProcess

class SplashActivity : AppCompatActivity() {

    private val neededPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        ActivityCompat.requestPermissions(this, neededPermissions, Config.GET_PERMISSIONS)
    }

    private fun permission() {
        AlertDialog.Builder(this)
            .setTitle("获取权限")
            .setMessage(
                "本软件一共需要获取6项权限，只有权限全部通过才能正常运行\n" +
                        "\n" +
                        "需要获取的权限如下\n" +
                        "1.摄像头：从摄像头获取人脸并用于识别，软件不会保存照片而是将人脸信息解析成特殊的二进制码\n" +
                        "2.读取手机状态：部分手机显示为读取通话记录，实际上读取手机唯一标识码并用于软件的激活\n" +
                        "3.互联网：应虹软公司要求，对识别引擎进行联网激活。软件不会向开发者的服务器传送包含隐私的信息，因为我根本没有服务器\n" +
                        "4.闪光灯：用于闪光灯提醒\n" +
                        "5.读取数据：用于读取数据库\n" +
                        "6.写入数据：用于写入数据库\n" +
                        "\n" +
                        "为了保证软件的正常运行，请全部授权"
            )
            .setPositiveButton("授权") { dialog, _ ->
                dialog.dismiss()
                ActivityCompat.requestPermissions(this, neededPermissions, Config.GET_PERMISSIONS)
            }
            .setNegativeButton("拒绝") { dialog, _ ->
                dialog.dismiss()
                finish()
                exitProcess(0)
            }
            .setCancelable(false)
            .show()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Config.GET_PERMISSIONS) {
            var allGranted = true
            for (neededPermission in neededPermissions) {
                allGranted = allGranted and (ContextCompat.checkSelfPermission(
                    this,
                    neededPermission
                ) == PackageManager.PERMISSION_GRANTED)
            }
            if (allGranted) {
                Handler().postDelayed({
                    val mainIntent = Intent(
                        this@SplashActivity,
                        MainActivity::class.java
                    )
                    startActivity(mainIntent)
                    finish()
                    overridePendingTransition(R.anim.activity_open, R.anim.activity_close)
                }, 500)
            } else {
                Toast.makeText(this, "权限没有通过", Toast.LENGTH_SHORT).show()
                permission()
            }
        }
    }

}