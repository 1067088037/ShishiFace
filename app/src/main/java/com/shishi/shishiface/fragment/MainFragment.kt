package com.shishi.shishiface.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.shishi.shishiface.Config

import com.shishi.shishiface.R
import com.shishi.shishiface.activity.FaceRecognitionActivity
import com.shishi.shishiface.faceserver.DataBase
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : androidx.fragment.app.Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private val db: DataBase
        get() = DataBase.localDataBase!!
    private var clickTime = Array<Long>(2) { 0 }
    private var oneStepFinished = false
    private var hidden = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.findViewById<ImageView>(R.id.ShishiView).setOnClickListener {
            val intent = Intent(activity!!, FaceRecognitionActivity::class.java)
            intent.putExtra("state", Config.RECOGNITION)
            startActivity(intent)
        }
        transparentEgg.setOnClickListener {
            for (i in clickTime.size - 1 downTo 1) {
                clickTime[i] = clickTime[i - 1]
            }
            clickTime[0] = System.currentTimeMillis()
            if (clickTime[0] - clickTime[clickTime.size - 1] <= 500) {
                clickTime.forEachIndexed { index, _ -> clickTime[index] = 0 }//清除列表
                if (oneStepFinished) {
                    db.updateOrAddSettings(Config.EGG2, "1")
                    AlertDialog.Builder(activity!!)
                        .setTitle("彩蛋")
                        .setMessage("恭喜您找了一个彩蛋！\n剩余${Config.EGG_NUM - db.updateEggs()}个，继续努力！")
                        .setPositiveButton("好") { dialog, _ -> dialog.dismiss() }
                        .setCancelable(false)
                        .show()
                } else {
                    Toast.makeText(activity!!, "你也觉得这块地方很空旷吗？", Toast.LENGTH_SHORT).show()
                }
                oneStepFinished = true
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        this.hidden = hidden
        if (!hidden) {
            Thread {
                while (!this.hidden) {
                    try {
                        val isLoginInTimeOut = System.currentTimeMillis() > Config.loginInTimeoutAim
                        val restS = (Config.loginInTimeoutAim - System.currentTimeMillis()).toDouble() / (1000)
                        val restTime = if (!isLoginInTimeOut) String.format("%.0f", restS) else "0"
                        val text1 = "欢迎使用成都石室中学人脸识别系统\n请在左边的菜单栏中打开需要的功能"
                        val text2 = if (isLoginInTimeOut) ""//权限超时
                        else "\n已登录用户的权限: ${Config.permissionToText(Config.permissions)} (${restTime}秒)"
                        activity!!.runOnUiThread {
                            val text = "$text1 $text2"
                            mainWelcomeText.text = text
                        }
                    } finally {

                    }
                    Thread.sleep(500)
                }
            }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

}