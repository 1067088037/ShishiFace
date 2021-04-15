package com.shishi.shishiface.widget

import android.app.AlertDialog
import android.content.Context
import android.view.View
import com.shishi.shishiface.R
import android.content.DialogInterface
import android.graphics.Color
import android.widget.Switch
import com.arcsoft.face.LivenessInfo
import com.shishi.shishiface.Config
import com.shishi.shishiface.faceserver.DataBase
import kotlinx.android.synthetic.main.layout_liveness_demand.view.*
import kotlinx.android.synthetic.main.layout_liveness_demand.view.livenessDemadnShowMessage

class LivenessDemandAlertDialog constructor(context: Context) {

    private var view = View.inflate(context, R.layout.layout_liveness_demand, null)
    private var dialog: AlertDialog
    private val db: DataBase
        get() = DataBase.localDataBase!!
    private var title = "活体识别"
    private var message = "活体识别用于防止照片骗过人脸识别系统，请在下方设置需要开启活体识别的位置。\n" +
            "应虹软公司要求，活体识别自编译起有效期为1年，过期后将无法使用，过期后请安装新版ShishiFace。"
    private val needAlive = ArrayList<Int>()
    private val switch = arrayListOf<Switch>(view.liveness_demand_record, view.liveness_demand_permission, view.liveness_demand_recognition)
    private val intToBoolean = { int: Int -> int == 1 }
    private val booleanToInt = { boolean: Boolean -> if (boolean) 1 else 0 }

    init {
        for (i in Config.LIVENESS_DEMAND_INDEX) {
            needAlive.add(db.getOrAddSettings(i, "0").value.toInt())
        }
        for (i in switch.indices) {
            switch[i].isChecked = intToBoolean(needAlive[i])
        }
        view.liveness_demand_permission.setOnClickListener {
            if (Config.sysAdminIsAlive != LivenessInfo.ALIVE) {
                val text = "启动验证权限的活体识别请保证系统管理员在验证权限时为活体，否则可能导致权限丢失。\n" +
                        "要想开启此开关，您可以返回主页后在右上角菜单中注销权限，再以活体系统管理员身份重新进入设置。"
                view.livenessDemadnShowMessage.text = text
                view.livenessDemadnShowMessage.setTextColor(Color.RED)
                view.liveness_demand_permission.isChecked = false
            }
        }
        view.livenessDemadnShowMessage.text = message
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setTitle(title)
            .setNegativeButton("取消") { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton("确认", null)
            .setCancelable(true)
            .create()
        dialog.setOnCancelListener {

        }
    }

    fun show() {
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            for (i in switch.indices) {
                db.updateSettings(Config.LIVENESS_DEMAND_INDEX[i], db.booleanToString(switch[i].isChecked))
            }
            dialog.cancel()
        }
    }

}