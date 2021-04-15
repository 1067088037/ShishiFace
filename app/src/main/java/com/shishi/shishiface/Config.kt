package com.shishi.shishiface

import android.content.Context
import android.graphics.Color
import android.hardware.Camera
import android.telephony.TelephonyManager
import android.util.Log
import com.arcsoft.face.*
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.shishi.shishiface.faceserver.DataBase
import java.math.BigInteger
import kotlin.math.max

object Config {

    const val APP_ID = "DN8HWuhXRSBmqvrMQZQK6SCJ7w2i8cnBZ5BvpueVzCGY"
    const val SDK_KEY = "E5bosYy8F3YnvncJpz9XE3FpqkDK2XHAV43hF6sgFVmW"
    const val MIN_ALLOWED_VALUE = 0.8//识别成功的最低数值
    const val CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT
    const val DRAW_COLOR = Color.WHITE
    const val DRAW_BACKGROUND_COLOR = Color.WHITE
    const val VERSION = BuildConfig.VERSION_NAME

    const val UNKNOWN = -1//未知

    const val GUEST = 1//来宾用户
    const val ADMINISTRATOR = 2//管理员
    const val SYSTEM_ADMINISTRATOR = 3//系统管理员
    const val DEVELOPER = 4//开发者权限

    const val UNKNOWN_STATE = 100//未知状态
    const val GET_PERMISSIONS = 101//获取权限
    const val GET_FACE = 102//获取人脸
    const val RECORD_FACE = 105//录入人脸
    const val EXIT = 108//关闭
    const val FIRST_USE = 109//首次使用
    const val RECOGNITION = 110//正式识别
    const val SUCCESS = 111//成功
    const val CHANGE_INFO = 112//修改信息
    const val INPUT_FACE = 113//导入人脸
    const val READ_FACE = 114//解读人脸
    const val ABOUT = 115//关于
    const val ACCOUNT = 116//账户
    const val SETTINGS = 117//进入设置
    const val FAULT = 118//失败
    const val INPUT_DB = 119//导入数据库
    const val ALLOWED_DISTANCE = 120//允许的距离
    const val TEST_DISTANCE = 121//测试距离

    const val EGG1 = 201//彩蛋1
    const val EGG2 = 202//彩蛋2
    const val EGG3 = 203//彩蛋3
    val EGG_INDEX = arrayListOf(EGG1, EGG2, EGG3)
    val EGG_NUM = EGG_INDEX.size//彩蛋个数

    const val ALERT_MODE = 301//提醒方式 0.文字 1.文字和声音 2.文字声音闪光灯
    //302不允许使用
    const val LIVENESS_DEMAND_RECORD = 303//活体识别需求：录入 0.不需要 1.强制需要
    const val LIVENESS_DEMAND_PERMISSION = 304//活体识别需求：权限 0.不需要 1.强制需要
    const val LIVENESS_DEMAND_RECOGNITION = 305//活体识别需求：识别 0.不需要 1.强制需要
    val LIVENESS_DEMAND_INDEX =
        arrayListOf(LIVENESS_DEMAND_RECORD, LIVENESS_DEMAND_PERMISSION, LIVENESS_DEMAND_RECOGNITION)
    const val RECOGNITION_DIRECTION = 306//人脸识别方向 0.0° 1.90° 2.180° 3.270° 4.全向
    const val SYS_ADMIN_PASSWORD = 307//系统管理员密码，String量
    const val ENCRTPTION = 308//启动加密 0.关闭 1.启动
    const val LOGIN_IN_TIMEOUT = 309//登录超时 数字是分钟数

    //开发者密钥
    const val DEVELOPER_PASSWORD = "ShishiHighSchool"
    private const val DEVELOPER_KEY =
        "0000fa4400007443de01a73d59819abce25fc73b48a9b5bc2720cdbd67853dbd27717d3dd777293caf27883d6da3dabd34294bbd15b1213d3d631dbd299dbb3bb4329cb80af9e63c495d803d8ac182bd0ae5f73c3ac590bc29dcac3d181157bde246c13b58db9ebdb7248fbdba4fa63d0581aebcf94b853de77738bb93f6253da19f043b14bfac3dccf277bd663f5bbc0aa83dbc8d91853c4ca063bd70671d3b27223fbd5be19fbd0fa3e8bdabc5eabd93d1083d518b72bdf31990bdd1029d3d7e80fb3d7399d0bd8f8098bcc424af3ce4965a3cf62f27bd1f2ae6bc4a75353c82894ebd8254e33b0cb4d6bd4915f3bd3f7ee13c22d69a3c52ee2b3e98b0af3cf4ff4f3d1c47acbd4662a4bde54a933d8e137fbdfce88f3dc3794abda6e6c43d28721dbd5aa81d3d4f4f81bdefecb43ddcea46bda58b223e1b1ddcbc0f1626bdb6ab033ea09ca93d8b2b5b3d2813eebc30df56bc41d7a13dcc0c2d3dc696043d19fa2c3c3de8d93c71c5b93dbd5d0d3bb4066b3dcc9ab63d61ef0bbede3afabcd533893ba14c80bd0fa2f8bde88cda3c603c6b3dfc2acdbd872917bdb98f1c3ddba9143bcd245cbd5ee0293c91455cbc0e2d9ebdbd0aa13dae6a92bb3df95dbcc3c9033c525e8abd6b328abd042b4bbcf3076dbd9706443dff21cb3cd0d790bd0725f93d56ad12bc7f77bbbb60ad0c3d633c933c3dbe63bd6434fbbd57bd333dd913543ccf57abbd49264f3b20d7363d24b3143e39e3d13c7bdbd43ddc78afbd2e26b0bd5eb9b43d29aa1dbc9cd2683dabfff0bc493c7ebd9326c93d7c69223d0f4fe13c56fa1abd2e94ddbd72000fbd27a559bdc624afbdff1c16bd0a06d9bdbdfc2d3d0399c6bd75032cbdae74f23b9a7cc33aeb8ad63bb52078bd0f4ca03cb25747bc183fe23ca7c0883c878642bdba0608be857ae6bbff3ca8bd89c30dbd702c073cbeecd8bb99c93f3cdfa7163eeee347bea44d93bd0d04acbc371e51bd6c87473d68f2963d28aa08bdb6ffb5bdf06a8bbd2219143df976eebb429f443d7f6c30bcdb5acdbd99ed263d2e21813d9ccc88bc7055a73d2af20cbd983c9c3d1514c03c47be28bb009cd8bd6f90093ce0b324bd165c85bd559ae1bc75ca77bc761e88bd31b8e33d0a0f02bdf72ade3b8dd223bc60dde4bc81e31d3b2e2e873cc4acc2bc1e5eaebd2a12f6bc33db653b0137c6bc0613a5bc64fc9d3b21275c3d5031153dff78c3bc7598a53dbcc73ebc76826bbd3776b23dc46ef1bde5687fbcdb3735bd1911acbdbde551bd6ad76c3d6a270ebe05598bbd83bf8cbd82043bbbb370903b5c693fbdd8d9a2bd443522bcaba1ec3c9c61b03c82d67e3c3d175cbdc664a03de5ff13bcf0521c3dcd4bc3bb9fb3133d827ec5bcffb6c83b3d43c53d0f55883d909bce3dcc6074bd9afb903da85ffbbcab3621bddf3064bd1e39b4bc464fe4bdbe64c4bc"
    val DEVELOPER_FEATURE = hexStringToBytes(DEVELOPER_KEY)
    private const val CHAIRMAN_MAO_KEY =
        "0000fa44000074432d06253cfef8353e32d394bdeb9be8bcca233e3d803e1ebe09a4fe3dd3534bbb45039abcf21dc63c2eaf88bde756c33b618cb43d382b4ebd4b1facbde91ac53cb60d533ddbc7993aff1d823d94bef83df1672d3cd51225bdadf6293de094e23b65d4013d296a633d239614bdba80883de2123ebcba1b7dbc9796b8bd307e97bdedd33abdd8adbfbd889545bc39be16be74948bbda5118cbdcfec8bbd1ac898bc6cfca2bd667cdebcd762c83d1f161a3d3e756abd1c4fc4bd0f086ebd37a95c3d629fc83bad624ebdd0fd733d7ab4fabde5d4153de2825ebdad941c3dc875123dd243e33d135aac3d90be3fbd34918c3d3ced36bdfa2bf83dc8083b3d5ed6a0bc7a02b9bd74aa97bd7d004c3d4605c3bd9740363d3acaad3d287f1e3cc609da3d8306bc3c3c3e85bd6f1bc53d7300253decf6833cd8818f3db31c033ec4848bbd0012633dcc25d43cc945093d4674b23c6dd153bdb2b671bc61abba3adae3193ce08e40bdd0fbcd3cac0b9d3cc5e3b3bd883c5cbda2a78fbdb678a1bc772a253b1d1b0f3e7a1387bd537f073d46feee3c80da86ba11e5b3bdcf58cabdd3e7f7bd00737abd37c386bdb897c23d8f5560bd07dd853c6110a83d74bbc6bdecaec03d5e193c3d705ba53d8635703d86016ebdfadf52bbaf90a1bd25417a3d3b91f03d1b35e0bcb4cf22bd3ab3fb3cd47f95bc4665dcbc845e2c3def5b1b3bea96ed3b5b75f3bcb97052bd9887143e52c58d3dc51ce1bd28df8ebdd050c83b7ceb12bc8c8ba4bc29f2f33bc64a1dbc62ee6e3d00bdc33de5c067bd29661dbd542a043d6dc6c9bcccd7b13b8063ccbd9b5754bd0b5e2dbd3fd4f73cead29ebcb93d293ddec1ecbc0e0cd03d98e971bd27cd0d39279daf3dfc69183dd5f0f4bc87ba1cbd1a7ca53c71fefebc933e5b3d6ae977bd5da5083aa538d7bdc10dfb3d7a43083ce0e316bd18ba4abc6497183d1408b6bdfcb0623b9988913d3d49d6bc6c501cbdeda68439b2bc2e3df33b98bca92632bd8148bf3d847ce6bc3a19083e30490b3d601ad43d9b528c3c269199bd216e243c3f2a91bd27c842bcd1453d3df376ce3b755f9b3c0e0d193e71ee003cce4874bd40d2003e85532b3d9f160fbc368bf13cdd9e85bda8fdd23ccf7ce63b93fe76bdc16e653da581193d9504e43dc78e6a3db3cd173ec30dda3ccf8b0abbd8ea4c3db6f2133d53edb6bd43a2d3bc5a692ebd1fb65f3d645b223daba411bd42cacebc857cbebddcf5cc3be41a653a63c401bd99236a3d771bf6bc6f17793d40f8aabd1e6916bdfc07fc3bea116bbd8d7cc2bdf934813b21cae03d21ca04bdde95453b0ac9823d4519ab3d7511abbdbba89dbc5234063dbe98253da0cf4c3d4c317e3d2dd88bbd6270b5bb44a115bc04886fbd628e0dbd42bb8abc445991bc2097e4bcc350dc3bc87de5bc47c983bc43d66fbd"
    val CHAIRMAN_MAO_FEATURE = hexStringToBytes(CHAIRMAN_MAO_KEY)
    private const val publicPassword = 0x47c983bc43d66fbd//加密公钥，来自毛主席人脸特征最后8位的16进制

    var firstUse = true//首次使用
    var permissions: Int = 0
        set(value) {
            loginInTimeoutAim = if (value == GUEST) 0
            else System.currentTimeMillis() + loginInTimeoutTime * 1000
            field = value
//            Log.d("石室", "权限:$field, 过期时间:$loginInTimeoutAim, 当前时间:${System.currentTimeMillis()}")
        }
        get() {
            if (System.currentTimeMillis() > loginInTimeoutAim) field = GUEST //超时销毁权限
//            Log.d("石室", "权限:$field, 过期时间:$loginInTimeoutAim, 当前时间:${System.currentTimeMillis()}")
            return field
        }
    private val loginInTimeoutTime: Int
        //返回秒
        get() {
            val t = DataBase.localDataBase?.getSettings(LOGIN_IN_TIMEOUT)?.value?.toInt() ?: 60
            return max(t, 5)
        }
    var loginInTimeoutAim = System.currentTimeMillis()
    var sysAdminIsAlive = LivenessInfo.UNKNOWN//系统管理员在验证权限时为活体
    var state = UNKNOWN_STATE
    var direction: String = FaceEngine.ASF_OC_270.toString()//2020年10月10日 修改过
    var imei = ""
        //手机的IMEI，用于创建AES-256加密密码
        get() {
//            if (field.length != 15) {
//                TODO("无法读取手机标识码，拒绝启动数据库加密")
//            }
//            return field
            return "0"
        }
    var localDBPath = ""
    val defaultPassword: String
        get() = BigInteger(imei).multiply(BigInteger(publicPassword.toString())).toString()
    var cameraWidth: Int = 0
    val minAllowdWidth: Int
        get() {
            val i = (DataBase.localDataBase?.getOrAddSettings(
                ALLOWED_DISTANCE,
                "25"
            )?.value?.toDouble()!! / 100.0 * cameraWidth).toInt()
            return i
        }

    private var faceEngine: FaceEngine? = null
    private var faceEngineList = ArrayList<FaceEngine>()
    var listThreadNum = Runtime.getRuntime().availableProcessors()//List中线程数
    private var afCode = 0

    fun initEngine(context: Context?): FaceEngine {
        if (context != null && faceEngine == null) {
            faceEngine = FaceEngine()
            afCode = faceEngine!!.init(
                context,
                DetectMode.ASF_DETECT_MODE_VIDEO,
//                FaceEngine.ASF_DETECT_MODE_VIDEO,
//                direction.toInt(),
                DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                16,
                10,
                FaceEngine.ASF_FACE_RECOGNITION or FaceEngine.ASF_FACE_DETECT or FaceEngine.ASF_LIVENESS or FaceEngine.ASF_GENDER
            )
            val versionInfo = VersionInfo()
            FaceEngine.getVersion(versionInfo)
            Log.i("Config", "initEngine:  init: $afCode  version:$versionInfo")
        }
        return faceEngine!!
    }

    fun initEngineList(context: Context?): ArrayList<FaceEngine> {
        if (context != null) {
            if (faceEngineList.isEmpty()) {
                for (i in 0 until listThreadNum) {
                    faceEngineList.add(FaceEngine())
                    faceEngineList[i].init(
                        context,
                        DetectMode.ASF_DETECT_MODE_VIDEO,
//                FaceEngine.ASF_DETECT_MODE_VIDEO,
//                direction.toInt(),
                        DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                        16,
                        10,
                        FaceEngine.ASF_FACE_RECOGNITION or FaceEngine.ASF_FACE_DETECT or FaceEngine.ASF_GENDER
                    )
                }
            }
        }
        return faceEngineList
    }

    /**
     * 列表引擎不会重启
     */
    fun restartEngine(context: Context): FaceEngine {
        faceEngine?.unInit()
        faceEngine = null
        Thread.sleep(10)
        return initEngine(context)
    }

    /**
     * 不销毁List
     */
    fun destroyEngine() {
        faceEngine?.unInit()
        faceEngine = null
    }

    fun destroyEngineAndList() {
        faceEngine?.unInit()
        faceEngine = null
        for (i in faceEngineList) {
            i.unInit()
        }
        faceEngineList = ArrayList()
    }

    /**
     * 获取IMEI信息
     */
    fun getIMEI(context: Context, slotId: Int): String {
        return try {
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val method = manager.javaClass.getMethod("getImei", Int::class.javaPrimitiveType)
            method.invoke(manager, slotId) as String
        } catch (e: Exception) {
            ""
        }
    }

    fun permissionToText(permission: Int): String {
        return when (permission) {
            1 -> "来宾用户"
            2 -> "管理员"
            3 -> "系统管理员"
            4 -> "开发者"
            else -> "未知"
        }
    }

    fun genderToText(gender: Int): String {
        return when (gender) {
            GenderInfo.MALE -> "男"
            GenderInfo.FEMALE -> "女"
            GenderInfo.UNKNOWN -> "未知"
            else -> "未知"
        }
    }

    //设置信息为开发者
    fun setDeveloperInfo(
        id: ArrayList<Int>,
        sid: ArrayList<Int>,
        name: ArrayList<String>,
        gender: ArrayList<Int>,
        permission: ArrayList<Int>,
        i: Int
    ) {
        if (id.isEmpty()) {
            id.add(0)
            sid.add(20200301)
            name.add("开发者")
            gender.add(GenderInfo.MALE)
            permission.add(DEVELOPER)
        } else {
            id[i] = 0
            sid[i] = 20200301
            name[i] = "开发者"
            gender[i] = GenderInfo.MALE
            permission[i] = DEVELOPER
        }
    }

    //16进制字符串转byte数组
    fun hexStringToBytes(hexString_: String?): ByteArray? {
        var hexString = hexString_
        if (hexString == null || hexString == "") {
            return null
        }
        hexString = hexString.toUpperCase()
        val length = hexString.length / 2
        val hexChars = hexString.toCharArray()
        val d = ByteArray(length)
        fun charToByte(c: Char): Byte {
            return "0123456789ABCDEF".indexOf(c).toByte()
        }
        for (i in 0..length - 1) {
            val pos = i * 2
            d[i] = (charToByte(hexChars[pos]).toInt() shl 4 or charToByte(hexChars[pos + 1]).toInt()).toByte()
        }
        return d
    }

    //byte数组转16进制字符串
    fun bytesToHexString(src: ByteArray?): String? {
        val stringBuilder = StringBuilder("")
        if (src == null || src.size <= 0) {
            return null
        }
        for (i in 0..src.size - 1) {
            val v = src[i].toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
        }
        return stringBuilder.toString()
    }

}