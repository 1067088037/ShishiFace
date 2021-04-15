package com.shishi.shishiface.widget

import android.content.Context
import android.view.View
import com.shishi.shishiface.R
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.widget.SeekBar
import android.widget.Toast
import com.shishi.shishiface.Config
import com.shishi.shishiface.activity.FaceRecognitionActivity
import com.shishi.shishiface.faceserver.DataBase
import kotlinx.android.synthetic.main.layout_allowed_distance.view.*

class AllowedDistanceAlertDialog constructor(private val context: Context, private val callBack: CallBack) {

    interface CallBack {
        fun callBack(message: Int)
    }

    private var view = View.inflate(context, R.layout.layout_allowed_distance, null)
    private var dialog: AlertDialog
    private val db: DataBase
        get() = DataBase.localDataBase!!
    private val title = "最大识别距离"
    private val text: String
        get() = "设置合适的识别距离可以防止远处的人脸被误识别。当距离超过设定值时，即使比对成功也会显示“距离太远”而非“识别成功”\n" +
                "数值越大表示需要站的更近才能识别成功，默认数值为25\n\n" +
                "当前的距离: ${view.allowed_distance.progress}%"
    private var changed = false

    init {
        view.allowed_distance.max = 50
        val startProgress = db.getOrAddSettings(Config.ALLOWED_DISTANCE, "25").value.toInt()
        view.allowed_distance.progress = startProgress
        view.allowed_distance_hint.text = text
        val onSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                view.allowed_distance_hint.text = text
                changed = true
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        }
        view.allowed_distance.setOnSeekBarChangeListener(onSeekBarChangeListener)
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setTitle(title)
            .setNegativeButton("关闭") { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton("应用", null)
            .setNeutralButton("应用并测试") { _, _ ->
                db.updateOrAddSettings(Config.ALLOWED_DISTANCE, view.allowed_distance.progress.toString())
                Toast.makeText(context, "开始测试", Toast.LENGTH_SHORT).show()
                callBack.callBack(Config.TEST_DISTANCE)
            }
            .setCancelable(true)
            .create()
    }

    fun show() {
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val progress = view.allowed_distance.progress
            db.updateOrAddSettings(Config.ALLOWED_DISTANCE, progress.toString())
            Toast.makeText(
                context,
                if (changed) "距离修改成功"
                else "没有变化", Toast.LENGTH_SHORT
            ).show()
            changed = false
        }
    }

}