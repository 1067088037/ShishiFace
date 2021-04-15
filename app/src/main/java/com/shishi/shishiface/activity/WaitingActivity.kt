package com.shishi.shishiface.activity

import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.arcsoft.face.*
import com.shishi.shishiface.Config
import com.shishi.shishiface.R
import com.shishi.shishiface.faceserver.DataBase
import com.shishi.shishiface.util.ImageUtil
import java.io.File
import java.lang.Exception

class WaitingActivity : AppCompatActivity() {

    private var running = true
    private var step = 1
    private var state = Config.UNKNOWN_STATE
    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var imageButton: ImageButton
    private val db: DataBase
        get() = DataBase.localDataBase!!

    private var faceEngine = ArrayList<FaceEngine>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        state = intent.getIntExtra("state", Config.UNKNOWN_STATE)
        textView = findViewById(R.id.waiting_text)
        progressBar = findViewById(R.id.waiting_progress)
        imageButton = findViewById(R.id.waiting_button)
        imageButton.setImageResource(R.drawable.begin)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = when (state) {
            Config.READ_FACE -> {
                val fileList = intent.getStringArrayListExtra("path")
                val file = intent.getStringExtra("file")
                val text = "此目录下共有${fileList.size}个文件\n请点击下方按钮进行下一步操作\n将文件命名为ID,姓名.后缀名的形式\n可以自动记录ID和姓名到数据库中\n" +
                        "注意：ID和姓名之间需要用英文逗号隔开\n例如：李华,20200001.jpg"
                textView.text = text
                imageButton.setOnClickListener {
                    fun step2() {
                        fun create(allChecked: Boolean) {
                            val content = Array(fileList.size) { i: Int ->
                                val name = File(fileList[i]).name
                                if (name.length > 40) "${name.subSequence(0, 40)} ..."
                                else name
                            }
                            val checked = BooleanArray(fileList.size) { allChecked }
                            AlertDialog.Builder(this)
                                .setTitle("请选择需要导入的图片")
                                .setMultiChoiceItems(content, checked) { _, which, isChecked ->
                                    checked[which] = isChecked
                                }
                                .setPositiveButton("导入") { _, _ ->
                                    var hasChecked = false
                                    checked.forEach {
                                        if (it) {
                                            hasChecked = true
                                            return@forEach
                                        }
                                    }
                                    if (!hasChecked) {
                                        Toast.makeText(this, "没有勾选", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val newFile = ArrayList<String>()
                                        for (i in checked.indices) {
                                            if (checked[i]) {
                                                newFile.add(fileList[i])
                                            }
                                        }
                                        step = 3
                                        processImage(newFile)
                                    }
                                }
                                .setNegativeButton("取消") { _, _ ->

                                }
                                .setNeutralButton(if (!allChecked) "全选" else "全不选") { _, _ ->
                                    create(!allChecked)
                                }
                                .show()
                        }
                        create(false)
                    }
                    when (step) {
                        1 -> {
                            AlertDialog.Builder(this)
                                .setTitle("请选择需要导入的内容")
                                .setMessage("你需要导入刚才选择的图片还是一并导入图片所在的整个目录？")
                                .setNegativeButton("仅照片") { _, _ ->
                                    step = 3
                                    processImage(arrayListOf(file))
                                }
                                .setPositiveButton("照片和目录") { _, _ ->
                                    step = 2
                                    step2()
                                }
                                .show()
                        }
                        2 -> {
                           step2()
                        }
                        3 -> {
                           Toast.makeText(this ,"线程正忙，点击左上角箭头可以结束解析线程", Toast.LENGTH_SHORT).show()
                        }
                        4 -> {
                            finish()
                        }
                    }
                }
                "读取人脸"
            }
            else -> "ShishiFace"
        }
        faceEngine = Config.initEngineList(this)
    }

    private fun processImage(path: ArrayList<String>) {
        val frStartTime = System.currentTimeMillis()
        //指挥线程，用于调度其他的子线程解析人脸
        Thread {
            var nonFaceNum = 0//没有找到人脸的照片数量
            var num = 0//总共解析成功的人脸个数
            val maxThreadNum = Config.listThreadNum//最大线程数，可以自行修改
            var lastRunning = -1//上次运行的线程index
            var finished = 0//已经结束人物并销毁的线程个数
            val thread = ArrayList<Thread>()//放置线程的List
            for (i in path.indices) {
                thread.add(Thread {
                    lastRunning ++
                    val result = processFun(path[i], i)//调用解析的方法
                    if (result != 0) {
                        //人脸数目不为0
                        num += result
                    } else {
                        //人脸数目为0
                        nonFaceNum ++
                    }
                    finished ++//已结束任务的线程+1
                })
            }//向List中添加线程
            while (running) {
                //指挥线程的主循环
                val prepare = lastRunning + 1//正在准备的线程index是上一个运行的+1
                if (prepare - finished < maxThreadNum) {
                    if (prepare in thread.indices) {
                        try {
                            if (!thread[prepare].isAlive) thread[prepare].start()//运行新线程
                        } catch (e: IllegalThreadStateException) {
                            e.printStackTrace()
                        }
                        Log.d("石室", "线程$prepare 启动")
                    }
                }
                if (finished >= thread.size) {
                    //所有子线程均完成任务
                    step = 4
                    runOnUiThread {
                        val timeMs = (System.currentTimeMillis()-frStartTime).toDouble()
                        val text = "所有人脸已完成解析并导入到数据库中\n" +
                                "${thread.size}张图片中共解析出${num}张人脸\n" +
                                "${nonFaceNum}张没有解析出人脸\n" +
                                "总耗时${(timeMs/1000+1).toInt()}秒，" +
                                "平均每张耗时${String.format("%.0f", timeMs/thread.size)}毫秒"
                        textView.text = text
                        progressBar.progress = 100
                    }
                    break
                }
                runOnUiThread {
                    val progress = (finished.toDouble() / thread.size.toDouble()) * 100//调整进度条
                    val text = "正在使用${maxThreadNum}线程解析人脸，进度${progress.toInt()}%\n" +
                            "已完成${finished}个，共${thread.size}个，耗时${((System.currentTimeMillis()-frStartTime).toDouble()/1000).toInt()}秒"
                    textView.text = text
                    progressBar.progress = progress.toInt()
                }
                Thread.sleep(10)//指挥线程休眠留出资源给子线程
            }
        }.start()//运行指挥线程
    }

    private fun processFun(path: String, index: Int): Int {
        val j = index % Config.listThreadNum //计算余数用于规划线程和引擎，引擎只有4个，线程有很多个
        val options = BitmapFactory.Options()
        options.inSampleSize = 4
        val bitmap1 = BitmapFactory.decodeFile(path, options)
        val faceInfoList = ArrayList<FaceInfo>()
        /**
         * 1.准备操作（校验，显示，获取BGR）
         */
        if (bitmap1 == null) {
            return 0
        } else {
            try {
                val bitmap = ImageUtil.alignBitmapForBgr24(bitmap1)
                if (bitmap == null) {
                    return 0
                } else {
                    val width = bitmap.width
                    val height = bitmap.height
                    //bitmap转bgr
                    val bgr24 = ImageUtil.bitmapToBgr(bitmap)
                    if (bgr24 == null) {
                        return 0
                    } else {
                        /**
                         * 2.成功获取到了BGR24 数据，开始人脸检测
                         */
                        faceEngine[j].detectFaces(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfoList)
                        // 3.绘制省去
                        /**
                         * 4.上一步已获取到人脸位置和角度信息，传入给process函数，进行年龄、性别、三维角度检测
                         */
                        faceEngine[j].process(
                            bgr24,
                            width,
                            height,
                            FaceEngine.CP_PAF_BGR24,
                            faceInfoList,
                            FaceEngine.ASF_GENDER
                        )
                        //性别信息结果
                        val genderInfoList = ArrayList<GenderInfo>()
                        val genderCode = faceEngine[j].getGender(genderInfoList)
                        if (genderCode != ErrorInfo.MOK) {
                            return 0
                        }
                        /**
                         * 5.年龄、性别、三维角度已获取成功，添加信息到提示文字中
                         */
                        if (faceInfoList.size > 0) {
                            val faceFeatures = arrayOfNulls<FaceFeature>(faceInfoList.size)
                            val extractFaceFeatureCodes = IntArray(faceInfoList.size)
                            for (i in faceInfoList.indices) {
                                faceFeatures[i] = FaceFeature()
                                //从图片解析出人脸特征数据
                                extractFaceFeatureCodes[i] = faceEngine[j].extractFaceFeature(
                                    bgr24, width, height, FaceEngine.CP_PAF_BGR24,
                                    faceInfoList[i], faceFeatures[i]
                                )
                                if (extractFaceFeatureCodes[i] != ErrorInfo.MOK) {
                                    return 0
                                } else {
                                    fun getInfoName(filename: String?): ArrayList<String> {
                                        val result = ArrayList<String>()
                                        if (filename != null && filename.isNotEmpty()) {
                                            val dot = filename.lastIndexOf('.')
                                            if (dot > -1 && dot < filename.length - 1) {
                                                val name1 = filename.substring(0, dot)
                                                if (name1.isNotEmpty()) {
                                                    val dot1 = name1.lastIndexOf(",")
                                                    if (dot1 > -1 && dot1 < name1.length - 1) {
                                                        var sid = name1.substring(0, dot1)
                                                        if (sid.length > 10) sid = sid.substring(0..9)
                                                        var name = name1.substring(dot1 + 1)
                                                        if (name.length > 10) name = name.substring(0..9)
                                                        result.add(sid)
                                                        result.add(name)
                                                        //成功
                                                    } else {
                                                        result.add(String.format("%.0f", Math.random() * 100000000))
                                                        result.add("NAMELESS")
                                                        //失败，用随机数
                                                    }
                                                } else {
                                                    result.add(String.format("%.0f", Math.random() * 100000000))
                                                    result.add("NAMELESS")
                                                    //失败，用随机数
                                                }
                                            }
                                        }
                                        return result
                                    }
                                    try {
                                        val info = getInfoName(File(path).name)
                                        val sid = info[0].toInt()
                                        val name = info[1]
                                        val gender = genderInfoList[i].gender
                                        db.addFace(DataBase.FaceInfo(0, sid, name, gender, Config.GUEST, faceFeatures[i]!!))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        return faceInfoList.size
                                    }
                                }
                            }
                        }
                    }
                }
                return faceInfoList.size
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "图片超出尺寸，导入失败", Toast.LENGTH_SHORT).show()
                }
                return 0
            }
        }
    }

    override fun onResume() {
        super.onResume()
        running = true
    }

    override fun onPause() {
        super.onPause()
        running = false
        Toast.makeText(this, "离开界面，自动停止", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
    }

    private fun exit() {
        if (step == 3) {
            AlertDialog.Builder(this)
                .setTitle("关闭")
                .setMessage("你确定要关闭这个界面吗，正在运行的解析线程也将被关闭")
                .setPositiveButton("确认关闭") { _, _ ->
                    finish()
                }
                .setNegativeButton("刚才手抖了") { _, _ ->

                }
                .show()
        } else finish()
    }

    override fun onBackPressed() {
        exit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> exit()
        }
        return super.onOptionsItemSelected(item)
    }

}