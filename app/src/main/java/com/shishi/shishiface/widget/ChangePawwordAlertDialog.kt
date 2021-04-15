package com.shishi.shishiface.widget

import android.content.Context
import android.view.View
import com.shishi.shishiface.R
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.view.inputmethod.EditorInfo
import com.shishi.shishiface.Config
import com.shishi.shishiface.faceserver.DataBase
import kotlinx.android.synthetic.main.layout_change_password.view.*

class ChangePawwordAlertDialog constructor(context: Context) {

    interface CallBack {
        fun callBack(message: Int)
    }

    private var view = View.inflate(context, R.layout.layout_change_password, null)
    private var dialog: AlertDialog
    private val db: DataBase
        get() = DataBase.localDataBase!!
    private val title = "修改系统管理员密码"

    init {
        val hasPassword = db.getOrAddSettings(Config.SYS_ADMIN_PASSWORD, "").value != ""
        view.hasSysAdminPassword.isChecked = hasPassword
        view.checkPassword.isEnabled = hasPassword
        view.checkPassword.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        view.hasSysAdminPassword.setOnClickListener {
            val enabled = view.hasSysAdminPassword.isChecked
            view.checkPassword.isEnabled = enabled
        }
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setTitle(title)
            .setNegativeButton("取消") { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton("修改", null)
            .setCancelable(true)
            .create()
        dialog.setOnCancelListener {

        }
    }

    fun show() {
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (view.hasSysAdminPassword.isChecked) {
                val password = view.checkPassword.text.toString()
                when (password.length) {
                    in -1..8 -> {
                        val text = "请保证密码长度不少于9位"
                        view.sysAdminPasswordHint.text = text
                        view.sysAdminPasswordHint.setTextColor(Color.RED)
                        return@setOnClickListener//防止取消
                    }
                    in 17..Int.MAX_VALUE -> {
                        val text = "请保证密码长度不超过16位"
                        view.sysAdminPasswordHint.text = text
                        view.sysAdminPasswordHint.setTextColor(Color.RED)
                        return@setOnClickListener//防止取消
                    }
                    else -> {
                        db.updateOrAddSettings(Config.SYS_ADMIN_PASSWORD, password)
                    }
                }
            } else {
                db.deleteSettings(Config.SYS_ADMIN_PASSWORD)
            }
            dialog.cancel()
        }
    }

}