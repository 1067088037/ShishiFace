package com.shishi.shishiface.widget

import android.content.Context
import android.view.View
import com.shishi.shishiface.R
import android.app.AlertDialog
import android.content.DialogInterface
import android.widget.Toast
import com.shishi.shishiface.Config
import com.shishi.shishiface.faceserver.DataBase
import kotlinx.android.synthetic.main.layout_password.view.*

class CheckPasswordAlertDialog constructor(private val context: Context, private val callBack: CallBack) {

    interface CallBack {
        fun callBack(message: Int)
    }

    private var view = View.inflate(context, R.layout.layout_password, null)
    private var dialog: AlertDialog
    private val db: DataBase
        get() = DataBase.localDataBase!!
    private val title = "验证密码"

    init {
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setTitle(title)
            .setNegativeButton("取消") { dialog, _ ->
                callBack.callBack(Config.FAULT)
                dialog.cancel()
            }
            .setPositiveButton("确定", null)
            .setCancelable(false)
            .create()
        dialog.setOnCancelListener {

        }
    }

    fun show() {
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (view.checkPassword.text.toString() == Config.DEVELOPER_PASSWORD) {
                callBack.callBack(Config.DEVELOPER)
                dialog.cancel()
                return@setOnClickListener
            }//开发者高于设置
            if (db.getOrAddSettings(Config.SYS_ADMIN_PASSWORD, "-1").value == "-1") {
                Toast.makeText(context, "系统管理员没有开启密码", Toast.LENGTH_SHORT).show()
                dialog.cancel()
                return@setOnClickListener
            } else when (view.checkPassword.text.toString()) {
                db.getOrAddSettings(Config.SYS_ADMIN_PASSWORD, "-1").value -> {
                    callBack.callBack(Config.SYSTEM_ADMINISTRATOR)
                    dialog.cancel()
                }
                else -> {
                    Toast.makeText(context, "密码错误", Toast.LENGTH_SHORT).show()
                    view.checkPassword.setText("")
                }
            }
        }
    }

}