package com.shishi.shishiface.fragment

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.shishi.shishiface.Config

import com.shishi.shishiface.R
import com.shishi.shishiface.faceserver.DataBase
import kotlinx.android.synthetic.main.fragment_account.*

class AccountFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private val db: DataBase
        get() = DataBase.localDataBase!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        changeSysAdmin.setOnClickListener {
            val face = db.getFace()
            face.sortBy { it.sid }
            var index = 0
            for (i in face.indices) {
                if (face[i].permission == Config.SYSTEM_ADMINISTRATOR) {
                    index = i
                    break
                }
            }
            val content = Array(face.size) { "#${face[it].sid} ${face[it].name}" }
            AlertDialog.Builder(activity!!)
                .setTitle("修改系统管理员")
                .setSingleChoiceItems(content, index) { _, which -> index = which }
                .setPositiveButton("确认修改") { dialog, _ ->
                    //降级旧管理员
                    db.updateFace(getSysAdmin()!!.id, permission = Config.GUEST)
                    //升级新管理员
                    db.updateFace(face[index].id, permission = Config.SYSTEM_ADMINISTRATOR)
                    val text1 = "当前系统管理员\n#${getSysAdmin()!!.sid} ${getSysAdmin()!!.name}"
                    sysAdminInfo.text = text1
                    Toast.makeText(activity!!, "修改成功！", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("取消") { _, _ -> }
                .show()
        }

        manageAdmin.setOnClickListener {
            fun create() {
                val admin = getAdmin()
                admin.sortBy { it!!.sid }
                val content = Array(admin.size) { "#${admin[it]!!.sid} ${admin[it]!!.name}" }
                val checked = BooleanArray(admin.size) { false }
                AlertDialog.Builder(activity!!)
                    .setTitle("编辑管理员")
                    .setMultiChoiceItems(content, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setPositiveButton("添加") { _, _ ->
                        val face = db.getFace()
                        face.sortBy { it.sid }
                        for (i in face.size - 1 downTo 0) {
                            if (face[i].permission >= Config.ADMINISTRATOR) face.remove(face[i])
                        }
                        val content1 = Array(face.size) { "#${face[it].sid} ${face[it].name}" }
                        AlertDialog.Builder(activity!!)
                            .setTitle("添加管理员")
                            .setItems(content1) { _, which ->
                                db.updateFace(face[which].id, permission = Config.ADMINISTRATOR)
                                Toast.makeText(activity!!, "添加成功！", Toast.LENGTH_SHORT).show()
                            }
                            .show()
                    }
                    .setNegativeButton("删除") { _, _ ->
                        for (i in checked) {
                            if (i) {
                                AlertDialog.Builder(activity!!)
                                    .setTitle("删除")
                                    .setMessage("你确定要删除已选择的管理员吗？\n删除后管理员将降级为来宾用户而不是从系统中彻底删除")
                                    .setPositiveButton("删除") { _, _ ->
                                        for (j in checked.indices) {
                                            db.updateFace(admin[j]!!.id, permission = Config.GUEST)
                                        }
                                    }
                                    .setNegativeButton("取消") { _, _ -> create() }
                                    .show()
                                break
                            }
                        }
                    }
                    .setNeutralButton("取消") { _, _ ->

                    }
                    .show()
            }
            create()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val text = "当前系统管理员\n#${getSysAdmin()?.sid} ${getSysAdmin()?.name}"
            sysAdminInfo.text = text
        }
    }

    private fun getSysAdmin(): DataBase.FaceInfo? {
        for (i in db.getFace()) {
            if (i.permission == Config.SYSTEM_ADMINISTRATOR) return i
        }
        return null
    }

    private fun getAdmin(): ArrayList<DataBase.FaceInfo?> {
        val admin = ArrayList<DataBase.FaceInfo?>()
        for (i in db.getFace()) {
            if (i.permission == Config.ADMINISTRATOR) admin.add(i)
        }
        return admin
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
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

}
