package com.shishi.shishiface.fragment

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.arcsoft.face.*
import com.shishi.shishiface.Config

import com.shishi.shishiface.R
import com.shishi.shishiface.activity.FaceRecognitionActivity
import com.shishi.shishiface.activity.ManageFaceActivity
import com.shishi.shishiface.activity.WaitingActivity
import com.shishi.shishiface.util.FileUtils
import com.shishi.shishiface.widget.FaceInfoAlertDialog
import java.io.File
import kotlin.collections.ArrayList

class ManageFaceFragment : androidx.fragment.app.Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    private lateinit var manageButton: Button
    private lateinit var addFromCamera: Button
    private lateinit var addFromFile: Button
    private lateinit var textView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manage_face, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        manageButton = activity!!.findViewById(R.id.manage_face_bt)
        addFromCamera = activity!!.findViewById(R.id.record_face_from_camera_bt)
        addFromFile = activity!!.findViewById(R.id.record_face_from_file_bt)
        textView = activity!!.findViewById(R.id.mf_text_view)

        addFromCamera.setOnClickListener {
            val intent = Intent(context, FaceRecognitionActivity::class.java)
            intent.putExtra("state", Config.RECORD_FACE)
            startActivityForResult(intent, Config.RECORD_FACE)
        }
        addFromFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, Config.INPUT_FACE)
        }
        manageButton.setOnClickListener {
            startActivity(Intent(context, ManageFaceActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Config.RECORD_FACE -> {
                if (data == null) return
                //记录人脸后进入填写信息页面
                val intent = Intent()
                intent.putExtra("state", Config.RECORD_FACE)
                intent.putExtra("success", data.getBooleanExtra("success", false))
                intent.putExtra("repeat", data.getBooleanExtra("repeat", false))
                if (data.getBooleanExtra("repeat", false)) {
                    intent.putExtra("repeatID", data.getIntExtra("repeatID", -1))
                    intent.putExtra("repeatName", data.getStringExtra("repeatName"))
                }
                intent.putExtra("gender", data.getIntExtra("gender", GenderInfo.UNKNOWN))
                intent.putExtra("feature", data.getByteArrayExtra("feature"))
                FaceInfoAlertDialog(activity!!, intent, null).show()
            }
            Config.INPUT_FACE -> {
                if (data?.data != null) {
                    val path = FileUtils.getFilePathByUri(activity!!, data.data!!)
                    try {
                        val file = File(path)
                        val files = File(file.parent).listFiles()
                        val fileList = ArrayList<String>()
                        for (i in files) {
                            fun isImageFile(filePath: String): Boolean {
                                val options = BitmapFactory.Options()
                                options.inJustDecodeBounds = true
                                BitmapFactory.decodeFile(filePath, options)
                                return options.outWidth != -1
                            }
                            if (isImageFile(i.path)) {
                                fileList.add(i.absolutePath)
                            }
                        }
                        if (fileList.isNotEmpty()) {
                            val intent = Intent(activity!!, WaitingActivity::class.java)
                            intent.putExtra("state", Config.READ_FACE)
                            intent.putExtra("path", fileList)
                            intent.putExtra("file", file.absolutePath)
                            startActivity(intent)
                        }
                    } catch (e: NullPointerException) {
                        Toast.makeText(activity!!, "空路径，请重试或更换文件管理器", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            }
        }
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
