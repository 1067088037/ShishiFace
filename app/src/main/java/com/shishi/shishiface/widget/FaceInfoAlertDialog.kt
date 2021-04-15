package com.shishi.shishiface.widget

import android.content.Context
import android.view.View
import com.shishi.shishiface.R
import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import com.arcsoft.face.FaceFeature
import com.arcsoft.face.GenderInfo
import com.shishi.shishiface.Config
import com.shishi.shishiface.faceserver.DataBase
import kotlinx.android.synthetic.main.layout_face_info.view.*

class FaceInfoAlertDialog constructor(private val context: Context, intent: Intent, private val callBack: CallBack?) {

    interface CallBack {
        fun callBack(message: Int, id: Int)
    }

    private var view = View.inflate(context, R.layout.layout_face_info, null)
    private lateinit var dialog: AlertDialog
    private var recordSuccess = false
    private var gender = GenderInfo.UNKNOWN
    private var feature: FaceFeature? = null
    private var repeat = false
    private var repeatId = -1
    private var repeatName = ""
    private var state = Config.UNKNOWN_STATE
    private val db: DataBase
        get() = DataBase.localDataBase!!
    private var id = 0
    private var title = "填写信息"
    private var message = "请根据提示填写ID和姓名"
    private var messageColor = Color.rgb(0x30, 0xa5, 0x56)

    init {
        view.getGender.isEnabled = false
        state = intent.getIntExtra("state", Config.UNKNOWN_STATE)
        when (state) {
            Config.RECORD_FACE -> {
                gender = intent.getIntExtra("gender", GenderInfo.UNKNOWN)
                view.getGender.hint = Config.genderToText(gender)
                recordSuccess = intent.getBooleanExtra("success", false)
                if (!recordSuccess) {
                    Toast.makeText(context, "记录失败", Toast.LENGTH_SHORT).show()
                    dialog.cancel()
                } else {
                    if (intent.getBooleanExtra("repeat", false)) {
                        repeat = true
                        repeatId = intent.getIntExtra("repeatID", -1)
                        repeatName = if (intent.getStringExtra("repeatName") != null) {
                            intent.getStringExtra("repeatName")
                        } else ""
                        message = "人脸已经注册，请不要重复注册\n已注册ID#$repeatId | 已注册姓名:$repeatName"
                        messageColor = Color.RED
                    }
                    feature = FaceFeature(intent.getByteArrayExtra("feature"))
                }
            }
            Config.CHANGE_INFO -> {
                title = "修改人脸信息"
                id = intent.getIntExtra("id", 0)
                message = "不可修改的唯一ID#$id"
                view.getSid.hint = intent.getIntExtra("sid", 0).toString()
                view.getName.hint = intent.getStringExtra("name")
                gender = intent.getIntExtra("gender", GenderInfo.UNKNOWN)
                view.getGender.hint = Config.genderToText(gender)
            }
            Config.FIRST_USE -> {
                gender = intent.getIntExtra("gender", GenderInfo.UNKNOWN)
                view.getGender.hint = Config.genderToText(gender)
                feature = FaceFeature(intent.getByteArrayExtra("feature"))
            }
        }

        view.livenessDemadnShowMessage.text = message
        view.livenessDemadnShowMessage.setTextColor(messageColor)
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setTitle(title)
            .setNegativeButton("取消") { dialog, _ ->
                callBack?.callBack(Config.FAULT, -1)
                dialog.cancel()
            }
            .setPositiveButton("确认", null)
            .setCancelable(false)
            .create()
        dialog.setOnCancelListener {

        }
    }

    fun show() {
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {

            fun textIsEmpty(): Boolean {
                return when {
                    view.getSid.text.isEmpty() -> {
                        Toast.makeText(context, "ID不能为空", Toast.LENGTH_SHORT).show()
                        true
                    }
                    view.getName.text.isEmpty() -> {
                        Toast.makeText(context, "姓名不能为空", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }

            if (view.getSid.text.length > 10) {
                Toast.makeText(context, "ID过长", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (view.getName.text.length > 10) {
                Toast.makeText(context, "姓名过长", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when (state) {
                Config.RECORD_FACE -> {
                    if (textIsEmpty()) return@setOnClickListener
                    db.addFace(
                        DataBase.FaceInfo(
                            0,
                            view.getSid.text.toString().toInt(),
                            view.getName.text.toString(),
                            gender,
                            Config.GUEST,
                            feature!!
                        )
                    )
                    Toast.makeText(context, "记录成功", Toast.LENGTH_SHORT).show()
                    callBack?.callBack(Config.SUCCESS, -1)
                    dialog.cancel()
                }
                Config.CHANGE_INFO -> {
                    val sid = if (view.getSid.text.isEmpty()) view.getSid.hint.toString().toInt()
                        else view.getSid.text.toString().toInt()
                    val name = if (view.getName.text.isEmpty()) view.getName.hint.toString()
                        else view.getName.text.toString()
                    db.updateFace(id, sid = sid, name = name)
                    Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                    callBack?.callBack(Config.SUCCESS, id)
                    dialog.cancel()
                }
                Config.FIRST_USE -> {
                    if (textIsEmpty()) return@setOnClickListener
                    db.addFace(
                        DataBase.FaceInfo(
                            0,
                            view.getSid.text.toString().toInt(),
                            view.getName.text.toString(),
                            gender,
                            Config.SYSTEM_ADMINISTRATOR,
                            feature!!
                        )
                    )
                    Toast.makeText(context, "系统管理员录入成功", Toast.LENGTH_SHORT).show()
                    callBack?.callBack(Config.SUCCESS, -1)
                    dialog.cancel()
                }
            }
        }
    }

}