package com.shishi.shishiface.activity

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.GenderInfo
import com.shishi.shishiface.Config
import com.shishi.shishiface.R
import com.shishi.shishiface.faceserver.DataBase
import com.shishi.shishiface.widget.FaceInfoAlertDialog
import com.shishi.shishiface.widget.FirstSettingsAlertDialog

class FirstUseActivity : AppCompatActivity() {

    private val db: DataBase
        get() = DataBase.localDataBase!!
    private val neededPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val callBack = object : FaceInfoAlertDialog.CallBack {
        override fun callBack(message: Int, id: Int) {
            when (message) {
                Config.SUCCESS -> firstSettings()
                Config.FAULT -> record()
                else -> record()
            }
        }
    }
    private val callback1 = object : FirstSettingsAlertDialog.CallBack {
        override fun callBack(message: Int) {
            when (message) {
                DialogInterface.BUTTON_POSITIVE -> finished()
                DialogInterface.BUTTON_NEGATIVE -> {
                    for (i in db.getFace()) {
                        db.deleteFace(i.id)
                    }
                    record()
                }
            }
        }
    }

    private fun welcome() {
        AlertDialog.Builder(this)
            .setTitle("欢迎")
            .setMessage(
                "欢迎使用成都石室中学人脸识别系统\n这是一个由学生开发的人脸识别系统，旨在更高效的对校园内学生进行管理\n" +
                        "感谢虹软科技股份有限公司 ArcSoft® 提供的人脸识别引擎 ArcFace\n现在软件将带领你完成首次使用时的配置"
            )
            .setPositiveButton("下一步") { dialog, _ ->
                dialog.dismiss()
                ActivityCompat.requestPermissions(this, neededPermissions, Config.GET_PERMISSIONS)
            }
            .setNegativeButton("关闭") { dialog, _ ->
                dialog.dismiss()
                Config.state = Config.EXIT
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun active(showFault: Boolean = false, activeCode1: Int = 0) {
        val activeDialog = AlertDialog.Builder(this)
            .setTitle("激活${if (showFault) "[本次失败]" else ""}")
            .setMessage(
                "${if (showFault) "本次激活失败，错误代码为: $activeCode1 \n请保证设备已连接到互联网！\n" else ""}应虹软公司要求，软件需要联网激活\n请检查网络通畅后点击激活"
            )
            .setPositiveButton("激活", null)
            .setNegativeButton("上一步") { dialog, _ ->
                dialog.dismiss()
                welcome()
            }
            .setCancelable(false)
            .create()

        activeDialog.show()
        activeDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val faceEngine = FaceEngine()
            val activeCode = FaceEngine.activeOnline(applicationContext, Config.APP_ID, Config.SDK_KEY)
            if (activeCode == ErrorInfo.MOK || activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                Toast.makeText(applicationContext, "激活成功！", Toast.LENGTH_SHORT).show()
                record()
            } else {
                active(true, activeCode)
            }
            activeDialog.dismiss()
        }
    }

    private fun record() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("录入系统管理员")
            .setMessage(
                "首次使用需要录入系统管理员\n" +
                        "系统管理员是本软件最高权限"
            )
            .setPositiveButton("录入") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, FaceRecognitionActivity::class.java)
                intent.putExtra("state", Config.FIRST_USE)
                startActivityForResult(intent, Config.FIRST_USE)
            }
            .setNegativeButton("返回") { dialog, _ ->
                dialog.dismiss()
                active()
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }

    private fun firstSettings() {
        FirstSettingsAlertDialog(this, callback1).show()
    }

    private fun finished() {
        AlertDialog.Builder(this)
            .setTitle("完成")
            .setMessage("你已经完成所有配置！")
            .setPositiveButton("结束") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_use)
        welcome()
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
                active()
            } else {
                Toast.makeText(this, "权限没有通过", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(this, neededPermissions, Config.GET_PERMISSIONS)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        fun getInfo() {
            val gender = data!!.getIntExtra("gender", GenderInfo.UNKNOWN)
            val feature = data.getByteArrayExtra("feature")
            val intent = Intent()
            intent.putExtra("state", Config.FIRST_USE)
            intent.putExtra("gender", gender)
            intent.putExtra("feature", feature)
            FaceInfoAlertDialog(this, intent, callBack).show()
        }

        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Config.FIRST_USE -> {
                if (resultCode == Config.SUCCESS) getInfo()
                else record()
            }
        }
    }

    override fun onBackPressed() {
        Config.state = Config.EXIT
        finish()
    }

}