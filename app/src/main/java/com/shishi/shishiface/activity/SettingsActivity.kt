package com.shishi.shishiface.activity

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.enums.DetectMode
import com.shishi.shishiface.Config
import com.shishi.shishiface.R
import com.shishi.shishiface.faceserver.DataBase
import com.shishi.shishiface.util.FileUtils
import com.shishi.shishiface.widget.AllowedDistanceAlertDialog
import com.shishi.shishiface.widget.ChangePawwordAlertDialog
import com.shishi.shishiface.widget.LivenessDemandAlertDialog
import kotlinx.android.synthetic.main.activity_settings.*
import net.sqlcipher.SQLException
import net.sqlcipher.database.SQLiteDatabase
import java.io.File
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private val db: DataBase
        get() = DataBase.localDataBase!!
    private val permission = Config.permissions
    private val checkPermission = { allowedMin: Int ->
        if (permission >= allowedMin) true
        else {
            runOnUiThread {
                Toast.makeText(this, "权限不足，请关闭此界面后更换高权限账户", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    private val distanceCallBack = object : AllowedDistanceAlertDialog.CallBack {
        override fun callBack(message: Int) {
            if (message == Config.TEST_DISTANCE) {
                val intent = Intent(applicationContext, FaceRecognitionActivity::class.java)
                intent.putExtra("state", Config.RECOGNITION)
                startActivityForResult(intent, Config.TEST_DISTANCE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.title = "设置"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        st_allowed_distance.setOnClickListener {
            allowedDistance()
        }

        st_manage_db.setOnClickListener {
            if (!checkPermission(Config.SYSTEM_ADMINISTRATOR)) {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("管理数据库")
                .setMessage(R.string.manageDBHint)
                .setPositiveButton("导出") { _, _ ->
                    //自动补0
                    fun autoAddZero(int: Int): String {
                        fun getLength(int: Int): Int {
                            var num = int
                            var count = 0
                            while (num >= 1) {
                                num /= 10
                                count++
                            }
                            return count
                        }
                        return if (getLength(int) < 2) "0$int" else int.toString()
                    }

                    //打开新的数据库
                    val filePath = File(Environment.getExternalStorageDirectory()!!.path + "/ShishiFace")
                    if (!filePath.exists()) {
                        filePath.mkdirs()
                    }
                    val calendar = Calendar.getInstance()
                    val fileName =
                        "Full_${calendar.get(Calendar.YEAR)}${autoAddZero(calendar.get(Calendar.MONTH) + 1)}${autoAddZero(
                            calendar.get(Calendar.DATE)
                        )}" + "_" +
                                "${autoAddZero(calendar.get(Calendar.HOUR_OF_DAY))}${autoAddZero(calendar.get(Calendar.MINUTE))}${autoAddZero(
                                    calendar.get(Calendar.SECOND)
                                )}"
                    val path = filePath.path + "/$fileName.db"

                    AlertDialog.Builder(this)
                        .setTitle("导出")
                        .setMessage("你确定要将[完整]的数据库导出到${filePath}文件夹中吗？\n文件名为$fileName.db\n导出的数据不会被加密！")
                        .setPositiveButton("导出") { _, _ ->
                            val face = db.getFace()
                            val settings = db.getSettings()
                            val db1 = DataBase.init(path, false)
                            for (i in face) {
                                db1.addFace(i)
                            }
                            for (i in settings) {
                                db1.addSettings(i.item, i.value)
                            }
                            db1.close()
                            Toast.makeText(this, "成功保存导出文件\n路径：$path", Toast.LENGTH_SHORT).show()
                        }//确定
                        .setNegativeButton("取消", null)
                        .show()
                }
                .setNegativeButton("导入") { _, _ ->
                    AlertDialog.Builder(this)
                        .setTitle("导入")
                        .setMessage("请选择一个数据库文件\n文件后缀名为.db")
                        .setPositiveButton("开始") { _, _ ->
                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            intent.type = "*/*"
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            startActivityForResult(intent, Config.INPUT_DB)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                .setNeutralButton("取消", null)
                .show()
        }

        st_encrypt_db.setOnClickListener {
            if (!checkPermission(Config.GUEST)) {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("数据库加密已开启")
                .setMessage(R.string.encryptDBHint)
                .setPositiveButton("好", null)
                .show()
        }

        st_direction.setOnClickListener {
            if (!checkPermission(Config.ADMINISTRATOR)) {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val content = arrayOf("左侧水平（0°）", "反向竖直（90°）", "右侧水平（180°）", "正向竖直（270° 默认）", "全向识别（不推荐）")
            val index = db.getOrAddSettings(Config.RECOGNITION_DIRECTION, "3").value.toInt()
            AlertDialog.Builder(this)
                .setTitle("请选择人脸识别的方向")
                .setSingleChoiceItems(content, index) { dialog: DialogInterface?, which: Int ->
                    db.updateSettings(Config.RECOGNITION_DIRECTION, which.toString())
                    Config.direction = when (which) {
                        0 -> FaceEngine.ASF_OC_0
                        1 -> FaceEngine.ASF_OC_90
                        2 -> FaceEngine.ASF_OC_180
                        3 -> FaceEngine.ASF_OC_270
                        4 -> FaceEngine.ASF_NONE
                        else -> FaceEngine.ASF_OC_270
                    }.toString()
                    Config.destroyEngine()
                    dialog!!.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        st_alert_mode.setOnClickListener {
            if (!checkPermission(Config.ADMINISTRATOR)) {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val content = arrayOf("仅文字（不推荐）", "文字和声音（默认）", "文字、声音和闪光灯")
            val index = db.getOrAddSettings(Config.ALERT_MODE, "1").value.toInt()
            AlertDialog.Builder(this)
                .setTitle("请选择识别成功时的提醒方式")
                .setSingleChoiceItems(content, index) { dialog: android.content.DialogInterface?, which: kotlin.Int ->
                    db.updateSettings(Config.ALERT_MODE, which.toString())
                    dialog!!.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        st_login_in_timeout.setOnClickListener {
            if (!checkPermission(Config.SYSTEM_ADMINISTRATOR)) {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val content = arrayOf("5秒", "1分钟（默认）", "2分钟", "5分钟", "10分钟")
            val index = when (db.getOrAddSettings(Config.LOGIN_IN_TIMEOUT, "60").value) {
                "5" -> 0
                "60" -> 1
                "120" -> 2
                "300" -> 3
                "600" -> 4
                else -> 1
            }
            AlertDialog.Builder(this)
                .setTitle("选择登录后权限不会被销毁的时间")
                .setSingleChoiceItems(content, index) { dialog, which ->
                    db.updateOrAddSettings(
                        Config.LOGIN_IN_TIMEOUT, when (which) {
                            0 -> 5
                            1 -> 60
                            2 -> 120
                            3 -> 300
                            4 -> 600
                            else -> 60
                        }.toString()
                    )
                    dialog.dismiss()
                }
                .setNegativeButton("取消") { _, _ -> }
                .show()
        }

        st_liveness_demand.setOnClickListener {
            if (!checkPermission(Config.SYSTEM_ADMINISTRATOR)) {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            LivenessDemandAlertDialog(this).show()
        }

        st_chaneg_password.setOnClickListener {
            if (!checkPermission(Config.SYSTEM_ADMINISTRATOR)) {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ChangePawwordAlertDialog(this).show()
        }

        st_wipe_all.setOnClickListener {
            if (!checkPermission(Config.SYSTEM_ADMINISTRATOR)) {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val content = arrayOf("我手滑了", "我确定清除所有数据")
            var which1 = 0
            AlertDialog.Builder(this)
                .setTitle("清除所有数据")
                .setSingleChoiceItems(content, which1) { _, which ->
                    which1 = which
                }
                .setPositiveButton("确定") { _, _ ->
                    if (which1 == 0) {
                        Toast.makeText(this, "那你下次不要再手滑了！", Toast.LENGTH_SHORT).show()
                    } else {
                        for (i in db.getFace()) {
                            db.deleteFace(i.id)
                        }
                        for (i in db.getSettings()) {
                            db.deleteSettings(i.item)
                        }
                        db.close()
                        Toast.makeText(this, "已清空所有数据！", Toast.LENGTH_SHORT).show()
                        Thread {
                            Thread.sleep(1000)
                            Config.state = Config.EXIT
                            finish()
                        }.start()
                    }
                }
                .setNegativeButton("取消") { _, _ -> }
                .show()
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Config.TEST_DISTANCE -> allowedDistance()
            Config.INPUT_DB -> {
                if (data?.data != null) {
                    val path = FileUtils.getFilePathByUri(this, data.data!!)
                    try {
                        fun getExtensionName(filename: String?): String? {
                            if (filename != null && filename.isNotEmpty()) {
                                val dot = filename.lastIndexOf('.')
                                if (dot > -1 && dot < filename.length - 1) {
                                    return filename.substring(dot + 1)
                                }
                            }
                            return filename
                        }
                        if (!getExtensionName(path).equals("db")) {
                            Toast.makeText(this, "$path 不是数据库文件", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                val db0 = SQLiteDatabase.openOrCreateDatabase(path, "", null)//仅仅是为了试试它有没有被加密
                                db0.close()
                                val db1 = DataBase.init(path, false)
                                val content = arrayOf("仅添加人脸", "仅覆盖人脸", "覆盖人脸和设置")
                                AlertDialog.Builder(this)
                                    .setTitle("请选择导入类型")
                                    .setItems(content) { _, which ->
                                        fun input(index: Int, sysAdminFromLocal: Boolean) {
                                            when (index) {
                                                0 -> {
                                                    if (sysAdminFromLocal.not()) {
                                                        db.getFace().forEach {
                                                            if (it.permission == Config.SYSTEM_ADMINISTRATOR)
                                                                it.permission = Config.ADMINISTRATOR
                                                        }
                                                    }
                                                    val face = db1.getFace()
                                                    for (i in face) {
                                                        if (i.permission == Config.SYSTEM_ADMINISTRATOR && sysAdminFromLocal) {
                                                            i.permission = Config.ADMINISTRATOR
                                                            db.addFace(i)
                                                        }
                                                    }
                                                }
                                                1 -> {
                                                    var sysAdmin: DataBase.FaceInfo? = null
                                                    db.getFace().forEach {
                                                        if (it.permission == Config.SYSTEM_ADMINISTRATOR && sysAdminFromLocal)
                                                            sysAdmin = it
                                                        db.deleteFace(it.id)
                                                    }//删除本地数据库的人脸
                                                    val face = db1.getFace()
                                                    for (i in face) {
                                                        if (i.permission == Config.SYSTEM_ADMINISTRATOR && sysAdminFromLocal) {
                                                            i.permission = Config.ADMINISTRATOR
                                                            db.addFace(i)
                                                            db.addFace(sysAdmin!!)
                                                        }
                                                    }
                                                }
                                                2 -> {
                                                    var sysAdmin: DataBase.FaceInfo? = null
                                                    db.getFace().forEach {
                                                        if (it.permission == Config.SYSTEM_ADMINISTRATOR && sysAdminFromLocal)
                                                            sysAdmin = it
                                                        db.deleteFace(it.id)
                                                    }//删除本地数据库的人脸
                                                    db.getSettings().forEach {
                                                        db.deleteSettings(it.item)
                                                    }//删除本地数据库的设置
                                                    val face = db1.getFace()
                                                    val settings = db1.getSettings()
                                                    for (i in face) {
                                                        if (i.permission == Config.SYSTEM_ADMINISTRATOR && sysAdminFromLocal) {
                                                            i.permission = Config.ADMINISTRATOR
                                                            db.addFace(i)
                                                            db.addFace(sysAdmin!!)
                                                        }
                                                    }
                                                    for (i in settings) {
                                                        db.addSettings(i.item, i.value)
                                                    }
                                                }
                                            }
                                            var sysAdminName = ""
                                            db.getFace().forEach {
                                                if (it.permission == Config.SYSTEM_ADMINISTRATOR)
                                                    sysAdminName = it.name
                                            }//删除本地数据库的人脸
                                            Toast.makeText(this, "导入成功\n系统管理员为$sysAdminName", Toast.LENGTH_SHORT).show()
                                            Config.permissions = Config.GUEST
                                            db1.close()
                                            finish()
                                        }
                                        AlertDialog.Builder(this)
                                            .setTitle("选择系统管理员")
                                            .setMessage("新的系统管理员来自")
                                            .setPositiveButton("本地数据库") { _, _ ->
                                                input(which,true)
                                            }
                                            .setNegativeButton("导入的数据库") { _, _ ->
                                                input(which,false)
                                            }
                                            .setNeutralButton("取消", null)
                                            .show()
                                    }
                                    .setNegativeButton("取消") { _, _ ->
                                        Toast.makeText(this, "导入已取消", Toast.LENGTH_SHORT).show()
                                    }
                                    .show()
                            } catch (e: SQLException) {
                                Toast.makeText(this, "无法读取数据库，原因可能是数据库被加密", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                    } catch (e: SQLException) {
                        Toast.makeText(this, "文件错误", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun allowedDistance() {
        if (!checkPermission(Config.ADMINISTRATOR)) {
            Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
            return
        }
        AllowedDistanceAlertDialog(this, distanceCallBack).show()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

}
