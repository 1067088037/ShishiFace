package com.shishi.shishiface.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.hardware.Camera
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.shishi.shishiface.util.face.FaceHelper
import com.arcsoft.face.*
import com.shishi.shishiface.Config
import com.shishi.shishiface.R
import com.shishi.shishiface.faceserver.*
import com.shishi.shishiface.log
import com.shishi.shishiface.model.DrawInfo
import com.shishi.shishiface.model.FacePreviewInfo
import com.shishi.shishiface.util.ConfigUtil
import com.shishi.shishiface.util.DrawHelper
import com.shishi.shishiface.util.camera.CameraHelper
import com.shishi.shishiface.util.camera.CameraListener
import com.shishi.shishiface.util.face.FaceListener
import com.shishi.shishiface.widget.CheckPasswordAlertDialog
import com.shishi.shishiface.widget.FaceRectView
import kotlinx.android.synthetic.main.activity_face_recognition.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.math.max

/**
 * 人脸识别Activity
 */
class FaceRecognitionActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {

    private val TAG = "FaceRecognitionActivity"
    private val MAX_DETECT_NUM = 3

    private var cameraHelper: CameraHelper? = null
    private var drawHelper: DrawHelper? = null
    private var previewSize: Camera.Size? = null
    private var textView: TextView? = null
    private var textViewLarge: TextView? = null
    private var imageView: ImageView? = null

    /**
     * 优先打开的摄像头
     */
    private val cameraID = Config.CAMERA_ID
    private var faceEngine: FaceEngine? = null
    private var faceHelper: FaceHelper? = null
    private var compareResultList: MutableList<CompareResult>? = null
    private var textOnTextView = ""
    private var textOnTextViewLarge = ""

    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    private var previewView: View? = null
    /**
     * 绘制人脸框的控件
     */
    private var faceRectView: FaceRectView? = null

    private val ACTION_REQUEST_PERMISSIONS = 0x001
    private val db: DataBase
        get() = DataBase.localDataBase!!
    private lateinit var chairmanMaoMusic: MediaPlayer
    private var lastFinishedTime = System.currentTimeMillis()
    private var state = Config.UNKNOWN_STATE
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var flashLight: FlashLight
    private var failedTimes = 0
    private var historyFaceFeature = Array<HistoryFaceFeature?>(10) { null }
    private var alertMode = "1"
    private var checkDeveloper = false

    private val passwordCallback = object : CheckPasswordAlertDialog.CallBack {
        override fun callBack(message: Int) {
            if (message != Config.FAULT) {
                val intent = Intent()
                intent.putExtra("permission", message)
                Config.permissions = message
                setResult(state, intent)
                finish()
            }
        }
    }

    /**
     * 所需的所有权限信息
     */
    private val NEEDED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_recognition)
        state = intent.getIntExtra("state", Config.UNKNOWN_STATE)

        supportActionBar?.title = when (state) {
            Config.RECOGNITION -> "人脸识别"
            Config.FIRST_USE -> "首次使用需要录入系统管理员"
            Config.GET_PERMISSIONS -> "验证权限"
            Config.RECORD_FACE -> "录入人脸"
            Config.UNKNOWN_STATE -> "未知状态"
            else -> "未知状态"
        }

        soundPlayer = SoundPlayer(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        faceEngine = Config.initEngine(this)

        //保持亮屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val attributes = window.attributes
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            window.attributes = attributes
        }

        // Activity启动后就锁定为启动时的方向
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        previewView = findViewById(R.id.texture_preview)
        //在布局结束后才做初始化操作
        previewView!!.viewTreeObserver.addOnGlobalLayoutListener(this)

        faceRectView = findViewById(R.id.face_rect_view)
        textView = findViewById(R.id.fr_text_view)
        textViewLarge = findViewById(R.id.fr_text_view_large)
        imageView = findViewById(R.id.fr_text_background)
        compareResultList = ArrayList()

        alertMode = db.getOrAddSettings(Config.ALERT_MODE, "1").value
        chairmanMaoMusic = MediaPlayer.create(this@FaceRecognitionActivity, R.raw.chairman_mao)
        chairmanMaoMusic.isLooping = false

        flashLight = FlashLight(this)
        imageView!!.alpha = 0.4f

        fr_text_background.setOnLongClickListener {
            checkDeveloper = true
            false
        }
    }

    override fun onResume() {
        super.onResume()
        imageView?.setImageResource(R.drawable.background2)
        onNoFace()
        cameraHelper?.start()
        checkDeveloper = false
    }

    override fun onPause() {
        super.onPause()
        cameraHelper?.stop()
        flashLight.changeFlashLight(false)
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        if (cameraHelper != null) {
            cameraHelper!!.release()
            cameraHelper = null
        }
        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        if (faceHelper != null) {
            faceHelper!!.release()
        }
        chairmanMaoMusic.release()
        soundPlayer.release()
        super.onDestroy()
    }

    private fun checkPermissions(neededPermissions: Array<String>?): Boolean {
        if (neededPermissions == null || neededPermissions.isEmpty()) {
            return true
        }
        var allGranted = true
        for (neededPermission in neededPermissions) {
            allGranted = allGranted and (ContextCompat.checkSelfPermission(
                this,
                neededPermission
            ) == PackageManager.PERMISSION_GRANTED)
        }
        return allGranted
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_face_recognition, menu)
        menu!!.findItem(R.id.use_password).isEnabled = state == Config.GET_PERMISSIONS
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.use_password -> {
                CheckPasswordAlertDialog(this, passwordCallback).show()
            }
            R.id.restart_engine -> restartEngine()
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initCamera() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        var lastFacePreviewInfoList: List<FacePreviewInfo> = ArrayList()
        var sameFace: Boolean = false
        var maoDismiss = true
        var name = ""//姓名
        var sameFaultTimes = 0

        val faceListener = object : FaceListener {
            override fun onFail(e: Exception) {
                Log.e(TAG, "onFail: " + e.message)
            }

            //请求FR的回调
            override fun onFaceFeatureInfoGet(faceFeature: FaceFeature?, msg: ArrayList<Int>?, startTime: Long) {
                if (msg == null) return
                val faceInfo = db.getFace()
                var maxFaceSimilar = 0.0f
                var maxPermission = Config.GUEST
                var resultID: Int = -1
                for (i in faceInfo.indices) {
                    val faceSimilar = FaceSimilar()
                    faceEngine!!.compareFaceFeature(faceFeature, faceInfo[i].feature, faceSimilar)
                    if (faceSimilar.score >= maxFaceSimilar) {
                        maxPermission = max(maxPermission, faceInfo[i].permission)
                        maxFaceSimilar = faceSimilar.score
                        resultID = i
                    }
                }
                textOnTextView = ""

                //开发者
                val faceSimilarDE = FaceSimilar()
                faceEngine!!.compareFaceFeature(faceFeature, FaceFeature(Config.DEVELOPER_FEATURE), faceSimilarDE)
                //毛主席
                val faceSimilarMao = FaceSimilar()
                faceEngine!!.compareFaceFeature(faceFeature, FaceFeature(Config.CHAIRMAN_MAO_FEATURE), faceSimilarMao)
                if (faceSimilarMao.score >= Config.MIN_ALLOWED_VALUE) {
                    name = "伟大的领袖毛主席"
                    if (maoDismiss) {
                        chairmanMaoMusic.start()
                        maoDismiss = false
                        db.updateOrAddSettings(Config.EGG3, "1")
                        runOnUiThread {
                            AlertDialog.Builder(this@FaceRecognitionActivity)
                                .setOnDismissListener { maoDismiss = true }
                                .setTitle("彩蛋")
                                .setMessage(
                                    "伟大的领袖毛主席，指引我们向前进！\n恭喜您找了一个彩蛋！\n" +
                                            "剩余${Config.EGG_NUM - db.updateEggs()}个，继续努力！"
                                )
                                .setPositiveButton("好") { _, _ -> }
                                .show()
                        }
                    }
                } else name = ""

                fun isAlive(item: Int): Boolean {
                    return db.getOrAddSettings(item, "0").value == "0" || (msg[2] == LivenessInfo.ALIVE)
                }

                fun onFirstUse() {
                    if (lastFacePreviewInfoList.isEmpty()) return
                    val intent = Intent()
                    intent.putExtra("gender", lastFacePreviewInfoList[0].genderInfo.gender)
                    intent.putExtra("feature", faceFeature!!.featureData)
                    setResult(Config.SUCCESS, intent)
                    Thread {
                        Thread.sleep(1000)
                        finish()
                    }.start()
                }

                fun onRecordFace(repeat: Boolean) {
                    if (lastFacePreviewInfoList.isEmpty()) return
                    val intent = Intent()
                    intent.putExtra("success", true)
                    intent.putExtra("repeat", repeat)
                    if (repeat) {
                        val list = db.getFace()
                        intent.putExtra("repeatID", list[resultID].sid.toInt())
                        intent.putExtra("repeatName", list[resultID].name)
                    }
                    intent.putExtra("gender", lastFacePreviewInfoList[0].genderInfo.gender)
                    intent.putExtra("feature", faceFeature!!.featureData)
                    setResult(Config.RECORD_FACE, intent)
                    Thread {
                        Thread.sleep(750)
                        finish()
                    }.start()
                }

                fun onGetPermissions(success: Boolean) {
                    if (lastFacePreviewInfoList.isEmpty()) return
                    val intent = Intent()
                    if (faceSimilarDE.score >= Config.MIN_ALLOWED_VALUE && checkDeveloper) {
                        Config.permissions = Config.DEVELOPER
                        intent.putExtra("permission", Config.DEVELOPER)
                        Config.sysAdminIsAlive = LivenessInfo.ALIVE
                    } else {
                        val permission = db.getFace()[resultID].permission
                        Config.permissions = permission
                        if (permission == Config.SYSTEM_ADMINISTRATOR) {
                            Config.sysAdminIsAlive = msg[2]
                        }
                        intent.putExtra("permission", permission)
                    }
                    if (success) {
                        setResult(state, intent)
                        Thread {
                            Thread.sleep(750)
                            finish()
                        }.start()
                    }
                }

                fun sound(index: Int, careSame: Boolean) {
                    if (alertMode == "0") return
                    if (!sameFace || !careSame) {
                        when (index) {
                            0 -> {
                                soundPlayer.fault.start()
                            }
                            1 -> {
                                soundPlayer.success.start()
                            }
                            2 -> {
                                soundPlayer.successShort.start()
                            }
                        }
                    }
                }

                val isNotAllowdWidth = "距离太远的人脸会被视为识别失败\n"
                var imageColor = Color.BLACK
                if (checkDeveloper && state == Config.GET_PERMISSIONS && faceSimilarDE.score >= Config.MIN_ALLOWED_VALUE
                    && msg[2] == LivenessInfo.ALIVE /*开发者强制要求活体识别*/) {
                    if (msg[0] >= Config.minAllowdWidth) {
                        sound(2, false)
                        onGetPermissions(true)
                        textOnTextViewLarge = Config.permissionToText(Config.DEVELOPER)
                        textOnTextView = "D E V E L O P E R"
                        imageColor = Color.GREEN
                    } else {
                        textOnTextViewLarge = "距离太远"
                        textOnTextView = isNotAllowdWidth
                        imageColor = Color.BLUE
                    }
                } else {
                    //识别成功
                    if (maxFaceSimilar >= Config.MIN_ALLOWED_VALUE) {
                        textOnTextViewLarge = if (msg[0] >= Config.minAllowdWidth) {
                            textOnTextView += "ID: ${faceInfo[resultID].sid} | 姓名: ${faceInfo[resultID].name} \n"
                            imageColor = Color.GREEN
                            when (state) {
                                Config.RECOGNITION -> {
                                    if (isAlive(Config.LIVENESS_DEMAND_RECOGNITION)) {
                                        //刷新历史列表
                                        fun refresh(cut: Int) {
                                            if (cut == -1) {
                                                //全系列刷新，依次向后移动一个
                                                for (i in historyFaceFeature.size - 1 downTo 1) {
                                                    if (historyFaceFeature[i] == null) break
                                                    historyFaceFeature[i] = historyFaceFeature[i - 1]
                                                }
                                                historyFaceFeature[0] =
                                                    HistoryFaceFeature(faceFeature!!, System.currentTimeMillis())
                                            } else {
                                                //切断某一个，并把它移动到第一个，前面的依次后移，后面的不动
                                                val historyTemp = historyFaceFeature[cut]
                                                for (i in cut downTo 1) {
                                                    if (historyFaceFeature[i] == null) break
                                                    historyFaceFeature[i] = historyFaceFeature[i - 1]
                                                }
                                                historyFaceFeature[0] = historyTemp
                                            }
                                        }

                                        val keepTime = 2000

                                        for (i in historyFaceFeature.indices) {
                                            if (historyFaceFeature[i] != null)
                                                if (System.currentTimeMillis() - historyFaceFeature[i]!!.time > keepTime)
                                                    historyFaceFeature[i] = null
                                        }

//                                        for (i in historyFaceFeature.indices) {
//                                            val j = historyFaceFeature[i]
//                                            if (j != null) log("序号$i:  ${System.currentTimeMillis()-j.time}")
//                                        }

                                        sameFace = false
                                        for (i in historyFaceFeature.indices) {
                                            if (historyFaceFeature[i] != null) {
                                                val j = historyFaceFeature[i]!!
                                                if (System.currentTimeMillis() - j.time <= keepTime) {
                                                    val faceSimilarLast = FaceSimilar()
                                                    faceEngine!!.compareFaceFeature(
                                                        faceFeature,
                                                        j.faceFeature,
                                                        faceSimilarLast
                                                    )
                                                    if (faceSimilarLast.score >= Config.MIN_ALLOWED_VALUE) {
                                                        j.time = System.currentTimeMillis()
                                                        refresh(i)
                                                        sameFace = true
                                                        break
                                                    }
                                                } else historyFaceFeature[i] = null
                                            }
                                        }

                                        if (!sameFace) {
                                            sameFaultTimes++
                                            sameFace = sameFaultTimes >= 3
                                            refresh(-1)
                                        } else sameFaultTimes = 0
                                        sound(1, true)
                                        if (alertMode == "2") flashLight.changeFlashLight(true)
                                        "识别成功"
                                    } else {
                                        imageColor = Color.BLUE
                                        "不是活体"
                                    }
                                }
                                Config.FIRST_USE -> {
                                    if (isAlive(Config.LIVENESS_DEMAND_RECORD)) {
                                        sound(2, false)
                                        onFirstUse()
                                        "录入成功"
                                    } else {
                                        imageColor = Color.BLUE
                                        "不是活体"
                                    }
                                }
                                Config.RECORD_FACE -> {
                                    sound(2, false)
                                    onRecordFace(true)
                                    "录入重复"
                                    //录入重复不需要检测活体
                                }
                                Config.GET_PERMISSIONS -> {
                                    if (isAlive(Config.LIVENESS_DEMAND_PERMISSION)) {
                                        sound(2, false)
                                        onGetPermissions(true)
                                        Config.permissionToText(db.getFace()[resultID].permission)
                                    } else {
                                        imageColor = Color.BLUE
                                        "不是活体"
                                    }
                                }
                                Config.UNKNOWN_STATE -> "未知状态"
                                else -> "未知状态"
                            }
                        } else {
                            imageColor = Color.BLUE
                            textOnTextView += isNotAllowdWidth
//                            lastFaceFeature = null
                            "距离太远"
                        }
                        failedTimes = 0
                        textOnTextView += "识别用时: ${System.currentTimeMillis() - startTime}毫秒"
                    } else {
                        //识别失败
                        flashLight.changeFlashLight(false)
                        textOnTextView = ""
                        textOnTextViewLarge = if (msg[0] >= Config.minAllowdWidth) {
                            when (state) {
                                Config.RECOGNITION -> {
                                    failedTimes++
                                    if (failedTimes >= 3) {
                                        imageColor = Color.RED
                                        sameFace = failedTimes != 3
                                        sound(0, true)
                                        "识别失败"
                                    } else {
                                        imageColor = Color.YELLOW
                                        textOnTextView = "本次识别失败，现在进行再次确认\n"
//                                        lastFaceFeature = null
                                        "再次确认"
                                    }
                                }
                                Config.FIRST_USE -> {
                                    sound(2, false)
                                    if (isAlive(Config.LIVENESS_DEMAND_RECORD)) {
                                        onFirstUse()
                                        imageColor = Color.GREEN
                                        "录入成功"
                                    } else {
                                        imageColor = Color.BLUE
                                        "不是活体"
                                    }
                                }
                                Config.RECORD_FACE -> {
                                    if (isAlive(Config.LIVENESS_DEMAND_RECORD)) {
                                        sound(2, false)
                                        onRecordFace(false)
                                        imageColor = Color.GREEN
                                        "录入成功"
                                    } else {
                                        imageColor = Color.BLUE
                                        "不是活体"
                                    }
                                }
                                Config.GET_PERMISSIONS -> {
                                    onGetPermissions(false)
                                    imageColor = Color.RED
                                    "验证失败"
                                }
                                Config.UNKNOWN_STATE -> {
                                    imageColor = Color.RED
                                    "未知状态"
                                }
                                else -> "未知状态"
                            }
                        } else {
                            imageColor = Color.BLUE
                            textOnTextView += isNotAllowdWidth
//                            lastFaceFeature = null
                            "距离太远"
                        }
                        textOnTextView += "识别用时: ${System.currentTimeMillis() - startTime}毫秒"
                    }
                }
                runOnUiThread {
                    textViewLarge!!.setTextColor(Color.WHITE)
                    imageView!!.setColorFilter(imageColor)
                    textViewLarge!!.text = textOnTextViewLarge
                    textView!!.text = textOnTextView
                }
                lastFinishedTime = System.currentTimeMillis()
            }
        }

        val cameraListener = object : CameraListener {
            override fun onCameraOpened(camera: Camera, cameraId: Int, displayOrientation: Int, isMirror: Boolean) {
                previewSize = camera.parameters.previewSize
                drawHelper = DrawHelper(
                    previewSize!!.width,
                    previewSize!!.height,
                    previewView!!.width,
                    previewView!!.height,
                    displayOrientation,
                    cameraId,
                    isMirror
                )
                Config.cameraWidth = previewSize!!.width

                faceHelper = FaceHelper.Builder()
                    .faceEngine(faceEngine!!)
                    .frThreadNum(MAX_DETECT_NUM)
                    .previewSize(previewSize!!)
                    .faceListener(faceListener)
                    .currentTrackId(ConfigUtil.getTrackId(this@FaceRecognitionActivity))
                    .build()
            }

            override fun onPreview(nv21: ByteArray, camera: Camera) {
                faceRectView?.clearFaceInfo()
                val facePreviewInfoList = faceHelper!!.onPreviewFrame(nv21)
                if (faceRectView != null && drawHelper != null) {
                    val drawInfoList = ArrayList<DrawInfo>()
                    for (i in facePreviewInfoList.indices) {
                        drawInfoList.add(
                            DrawInfo(
                                facePreviewInfoList[i].faceInfo.rect,
                                facePreviewInfoList[i].genderInfo.gender,
                                -1,
                                facePreviewInfoList[i].livenessInfo.liveness,
                                name
                            )
                        )
                    }
                    drawHelper!!.draw(faceRectView, drawInfoList)
                    lastFacePreviewInfoList = facePreviewInfoList
                }
                if (facePreviewInfoList.isNotEmpty() && previewSize != null) {
                    //有人脸
                    faceHelper!!.requestFaceFeature(
                        nv21,
                        facePreviewInfoList[0].faceInfo,
                        previewSize!!.width,
                        previewSize!!.height,
                        FaceEngine.CP_PAF_NV21,
                        facePreviewInfoList
                    )
                } else {
                    //无人脸
                    if (System.currentTimeMillis() - lastFinishedTime >= 1000) onNoFace()
                }
            }

            override fun onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ")
            }

            override fun onCameraError(e: Exception) {
                Log.i(TAG, "onCameraError: " + e.message)
            }

            override fun onCameraConfigurationChanged(cameraID: Int, displayOrientation: Int) {
                if (drawHelper != null) {
                    drawHelper!!.setCameraDisplayOrientation(displayOrientation)
                }
                Log.i(TAG, "onCameraConfigurationChanged: $cameraID  $displayOrientation")
            }
        }

        cameraHelper = CameraHelper.Builder()
            .previewViewSize(Point(previewView!!.measuredWidth, previewView!!.measuredHeight))
            .rotation(windowManager.defaultDisplay.rotation)
            .specificCameraId(cameraID)
            .isMirror(false)
            .previewOn(previewView)
            .cameraListener(cameraListener)
            .build()
        cameraHelper!!.init()
    }

    private fun restartEngine() {
        Thread {
            cameraHelper?.stop()
            onLowMemory()
            faceEngine = Config.restartEngine(applicationContext)
            onLowMemory()
            cameraHelper?.start()
            runOnUiThread {
                Toast.makeText(applicationContext, "重启成功", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun onNoFace() {
        failedTimes = 0
//        lastFaceFeature = null
        flashLight.changeFlashLight(false)
        runOnUiThread {
            textViewLarge!!.setTextColor(Color.WHITE)
            textViewLarge!!.text = when (state) {
                Config.RECOGNITION -> "没有人脸"
                Config.GET_PERMISSIONS -> "没有人脸"
                Config.FIRST_USE -> "没有人脸"
                Config.UNKNOWN_STATE -> "未知状态"
                else -> "没有人脸"
            }
            textView!!.text = "正在扫描"
            imageView!!.setColorFilter(
                when (state) {
                    Config.FIRST_USE -> Color.RED
                    else -> Color.BLACK
                }
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            var isAllGranted = true
            for (grantResult in grantResults) {
                isAllGranted = isAllGranted and (grantResult == PackageManager.PERMISSION_GRANTED)
            }
            if (isAllGranted) {
                initCamera()
                if (cameraHelper != null) {
                    cameraHelper!!.start()
                }
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 在[.previewView]第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    override fun onGlobalLayout() {
        previewView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS)
        } else {
            initCamera()
        }
    }

    data class HistoryFaceFeature(var faceFeature: FaceFeature, var time: Long)

}