package com.shishi.shishiface.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import com.arcsoft.face.FaceFeature
import com.arcsoft.face.FaceSimilar
import com.arcsoft.face.GenderInfo
import com.shishi.shishiface.Config
import com.shishi.shishiface.R
import com.shishi.shishiface.faceserver.DataBase
import com.shishi.shishiface.fragment.BlankFragment
import com.shishi.shishiface.fragment.FaceListFragment
import com.shishi.shishiface.fragment.RecyclerList
import com.shishi.shishiface.log
import com.shishi.shishiface.util.FileUtils
import com.shishi.shishiface.widget.FaceInfoAlertDialog
import kotlinx.android.synthetic.main.activity_manage_face.*
import net.sqlcipher.SQLException
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ManageFaceActivity : AppCompatActivity(), FaceListFragment.OnListFragmentInteractionListener, BlankFragment.OnFragmentInteractionListener {

    private val fragment: Int = R.id.face_list_fragment
    private lateinit var listFragment: FaceListFragment
    private val db: DataBase
        get() = DataBase.localDataBase!!
    private val callBack = object : FaceInfoAlertDialog.CallBack {
        override fun callBack(message: Int, id: Int) {
            if (message == Config.FAULT) return//失败不必刷新
            clearSearch()
            listFragment.updateFromDB()
            for (i in RecyclerList.faceItems.indices) {
                if (RecyclerList.faceItems[i].id == id) {
                    listFragment.myAdapter.change(i)
                    break
                }
            }
        }
    }

    private var lastClickItemTime: Long = 0
    private var allChecked = true
    private var searchText = ""
    private lateinit var allCheckedItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_face)

        RecyclerList.tempGroup = ArrayList()
        clearSearch()
        listFragment = FaceListFragment()
        supportFragmentManager.beginTransaction().replace(fragment, listFragment).commit()
        supportActionBar?.title = "管理现有人脸"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        showEmpty.alpha = 0.0f

        clear.setOnClickListener {
            search.setText("")
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    searchText = s.toString()
                    changeBySearch()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_manage_face, menu)
        allCheckedItem = menu!!.findItem(R.id.allChecked)
        return super.onCreateOptionsMenu(menu)
    }

    private fun changeBySearch() {
        val list = listFragment.updateFromDB()
        RecyclerList.release()
        for (i in list.indices) {
            list[i].checked = false
            val sid = list[i].sid.toString()
            val name = list[i].name
            val female = arrayOf("女", "雌", "母", "♀", "XX", "xx", "Xx", "xX")
            val male = arrayOf("男", "雄", "公", "♂", "XY", "Xy", "xY", "xy")
            val checkGender = { text: String ->
                var has = false
                when (list[i].gender) {
                    GenderInfo.FEMALE -> {
                        female.forEach {
                            if (it.indexOf(text) != -1) has = true
                        }
                        has
                    }
                    GenderInfo.MALE -> {
                        male.forEach {
                            if (it.indexOf(text) != -1) has = true
                        }
                        has
                    }
                    GenderInfo.UNKNOWN -> false
                    else -> false//不可能
                }
            }
            if (sid.indexOf(searchText) != -1 || name.indexOf(searchText) != -1 || checkGender(searchText)) {
                RecyclerList.addItem(list[i])
            }
        }
        RecyclerList.faceItems.sortWith(compareBy({ -it.permission }, { it.sid }))
        showEmpty.alpha = if (RecyclerList.faceItems.isEmpty()) 1.0f else 0.0f
        listFragment.myAdapter.changeAll()
    }

    private fun clearSearch() {
        showEmpty.alpha = 0.0f
        search.setText("")
        search.clearFocus()
    }

    override fun onListFragmentInteraction(
        item: RecyclerList.FaceItem?,
        act: FaceListFragment.OnListFragmentInteractionListener.Act
    ) {
        search.clearFocus()
        if (item == null) return
        when (act) {
            FaceListFragment.OnListFragmentInteractionListener.Act.Click -> {
                if (System.currentTimeMillis() - lastClickItemTime >= 2000) Toast.makeText(
                    this,
                    "长按可以对人脸信息进行修改\n标签[S]表示系统管理员\n标签[A]表示管理员\n没有标签表示来宾用户",
                    Toast.LENGTH_SHORT
                ).show()
                lastClickItemTime = System.currentTimeMillis()
            }
            FaceListFragment.OnListFragmentInteractionListener.Act.LongClick -> {
                val intent = Intent()
                intent.putExtra("state", Config.CHANGE_INFO)
                intent.putExtra("id", item.id)
                intent.putExtra("sid", item.sid)
                intent.putExtra("name", item.name)
                intent.putExtra("gender", item.gender)
                FaceInfoAlertDialog(this, intent, callBack).show()
            }
        }
    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Config.INPUT_FACE -> {
                if (data?.data != null) {
                    val path = FileUtils.getFilePathByUri(this, data.data!!)
                    try {
                        val faceEngine = Config.initEngine(this)
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
                                val db1 = DataBase.init(path, false)
                                val face1 = db1.getFace()
                                face1.sortBy { it.sid }
                                val face = db.getFace()
                                val id = IntArray(face1.size)
                                val content = Array(face1.size) { i -> i.toString() }
                                val checked = BooleanArray(face1.size) { true }
                                for (j in face1.indices) {
                                    id[j] = j
                                    content[j] = "#${face1[j].sid} ${face1[j].name}"
                                    for (i in face.indices) {
                                        val faceSimilar = FaceSimilar()
                                        faceEngine.compareFaceFeature(face[i].feature, face1[j].feature, faceSimilar)
                                        if (faceSimilar.score >= Config.MIN_ALLOWED_VALUE) {
                                            content[j] = "#${face1[j].sid} ${face1[j].name}" +
                                                    "\n[与#${face[i].sid} ${face[i].name}重复]"
                                            checked[j] = false
                                            break
                                        }
                                    }
                                }
                                AlertDialog.Builder(this)
                                    .setTitle("请选择需要导入的人脸\n已去除本地已保存人脸的勾选")
                                    .setMultiChoiceItems(content, checked) { _, which, isChecked ->
                                        checked[which] = isChecked
                                    }
                                    .setPositiveButton("导入") { _, _ ->
                                        for (j in id.indices) {
                                            if (checked[j]) {
                                                face1[j].permission = Config.GUEST
                                                val faceInfo = face1[j]
                                                db.addFace(faceInfo)
                                                listFragment.myAdapter.insert(
                                                    RecyclerList.FaceItem(
                                                        faceInfo.id,
                                                        faceInfo.sid,
                                                        faceInfo.name,
                                                        faceInfo.gender,
                                                        faceInfo.permission
                                                    )
                                                )
                                            }
                                        }
                                        listFragment.updateFromDB()
                                        clearSearch()
                                        Toast.makeText(this, "导入成功，权限全部设置为来宾用户", Toast.LENGTH_SHORT).show()
                                        allChecked = true
                                    }
                                    .setNegativeButton("取消") { _, _ ->
                                        Toast.makeText(this, "导入已取消", Toast.LENGTH_SHORT).show()
                                        clearSearch()
                                        allChecked = true
                                    }
                                    .show()
                                db1.close()
                            } catch (e: SQLException) {
                                Toast.makeText(this, "无法读取数据库，原因可能是数据库被加密", Toast.LENGTH_SHORT).show()
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        search.clearFocus()
        when (item?.itemId) {
            R.id.allChecked -> allChecked()
            R.id.reverseChoose -> reverseChoose()
            R.id.delete -> delete()
            R.id.input -> input()
            R.id.output -> output()
            R.id.add_temp_group -> addTempGroup()
            R.id.clear_temp_group -> clearTempGroup()
            R.id.temp_group -> tempGroup()
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun clearTempGroup() {
        AlertDialog.Builder(this)
            .setTitle("清空临时分组")
            .setMessage("您确定要清空临时分组吗？")
            .setPositiveButton("确定") { _, _ ->
                RecyclerList.tempGroup = ArrayList()

            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addTempGroup() {
        for (i in RecyclerList.faceItems) {
            if (i.checked && RecyclerList.tempGroup.indexOf(i) == -1)
                RecyclerList.tempGroup.add(i)
        }
    }

    private fun tempGroup() {
        val faceList = RecyclerList.tempGroup
        faceList.sortBy { it.sid }
        val content = Array(faceList.size) { "#${faceList[it].sid}  ${faceList[it].name}" }
        val checked = BooleanArray(faceList.size) { false }
        AlertDialog.Builder(this)
            .setTitle("临时分组")
            .setMultiChoiceItems(content, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("执行操作") { _, _ ->
                val content1 = arrayOf("删除", "导出")
                AlertDialog.Builder(this)
                    .setTitle("操作")
                    .setItems(content1) { _, which ->
                        val send = ArrayList<RecyclerList.FaceItem>()
                        for (i in checked.indices) {
                            if (checked[i])
                                send.add(faceList[i])
                        }
                        when (which) {
                            0 -> delete(send, 1)
                            1 -> output(send, 1)
                        }
                    }
                    .setNegativeButton("关闭", null)
                    .show()
            }
            .setNegativeButton("移除") { _, _ ->
                val list = Array(faceList.size) { faceList[it] }
                for (i in checked.indices) {
                    if (checked[i]) {
                        val face = list[i]
                        log(list.size)
                        val index = RecyclerList.tempGroup.indexOf(face)
                        if (index != -1)
                            RecyclerList.tempGroup.removeAt(index)
                    }
                }
            }
            .setNeutralButton("关闭", null)
            .show()
    }

    private fun allChecked() {
        for (i in RecyclerList.faceItems.indices) {
            RecyclerList.faceItems[i].checked = allChecked
        }
        allChecked = allChecked.not()
        allCheckedItem.title = if (allChecked) "全选" else "取消全选"
        listFragment.myAdapter.changeWhereToEnd(0)
    }

    private fun reverseChoose() {
        for (i in RecyclerList.faceItems.indices) {
            val checked = RecyclerList.faceItems[i].checked.not()
            RecyclerList.faceItems[i].checked = checked
        }
        listFragment.myAdapter.changeWhereToEnd(0)
    }

    private fun delete(list: ArrayList<RecyclerList.FaceItem> = RecyclerList.faceItems, index: Int = 0) {
        var j = 0
        var admin = 0
        for (i in list.indices) {
            if (list[i].checked) {
                j++
                if (list[i].isAdmin) {
                    admin++
                }
            }
        }
        if (j == 0) {
            Toast.makeText(this, "没有勾选", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("删除")
            .setMessage("你确定要永久删除已勾选的$j 张人脸吗？\n${if (admin != 0) "注意:其中$admin 张管理员人脸不会被删除\n删除管理员之前需要在账户管理界面注销权限" else ""}")
            .setPositiveButton("删除") { _, _ ->
                var min = list.size - 1
                for (i in list.size - 1 downTo 0) {
                    if (list[i].checked && !list[i].isAdmin) {
                        db.deleteFace(list[i].id)
                        listFragment.myAdapter.remove(i)
                        if (index == 1) RecyclerList.tempGroup.remove(list[i])
                        min = i
                    }
                }
                listFragment.updateFromDB()
                listFragment.myAdapter.changeWhereToEnd(min)
                clearSearch()
            }//确定
            .setNegativeButton("取消", null)
            .show()
    }

    private fun input() {
        AlertDialog.Builder(this)
            .setTitle("导入")
            .setMessage("请选择一个人脸识别数据库文件\n文件后缀名为.db")
            .setPositiveButton("开始") { _, _ ->
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(intent, Config.INPUT_FACE)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun output(list: ArrayList<RecyclerList.FaceItem> = RecyclerList.faceItems, index: Int = 0) {
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

        var j = 0
        for (i in list.indices) {
            if (list[i].checked) j++
        }
        if (j == 0) {
            Toast.makeText(this, "没有勾选", Toast.LENGTH_SHORT).show()
            return
        }

        //打开新的数据库
        val filePath = File(Environment.getExternalStorageDirectory()!!.path + "/ShishiFace")
        if (!filePath.exists()) {
            filePath.mkdirs()
        }
        val calendar = Calendar.getInstance()
        val fileName =
            "Face_${calendar.get(Calendar.YEAR)}${autoAddZero(calendar.get(Calendar.MONTH) + 1)}${autoAddZero(
                calendar.get(Calendar.DATE)
            )}" + "_" +
                    "${autoAddZero(calendar.get(Calendar.HOUR_OF_DAY))}${autoAddZero(calendar.get(Calendar.MINUTE))}${autoAddZero(
                        calendar.get(Calendar.SECOND)
                    )}"
        val path = filePath.path + "/$fileName.db"

        AlertDialog.Builder(this)
            .setTitle("导出")
            .setMessage("已勾选$j 张人脸\n你确定要将已勾选的人脸导出到${filePath}文件夹中吗？\n文件名为$fileName.db\n导出的数据不会被加密！")
            .setPositiveButton("导出") { _, _ ->
                val db1 = DataBase.init(path, false)
                when (index) {
                    0 -> {
                        val data = db.getFace()
                        for (i in RecyclerList.faceItems.indices) {
                            if (RecyclerList.faceItems[i].checked) {
                                db1.addFace(data[i])
                            }
                        }
                    }
                    1 -> {
                        val dbFace = db.getFace()
                        for (i in list) {
                            for (n in dbFace) {
                                if (i.id == n.id) {
                                    db1.addFace(n)
                                }
                            }
                        }
                    }
                }
                db1.close()
                Toast.makeText(this, "成功保存导出文件\n路径：$path", Toast.LENGTH_SHORT).show()
            }//确定
            .setNegativeButton("取消", null)
            .show()
    }

}