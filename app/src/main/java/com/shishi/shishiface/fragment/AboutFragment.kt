package com.shishi.shishiface.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.VersionInfo
import com.shishi.shishiface.BuildConfig
import com.shishi.shishiface.Config

import com.shishi.shishiface.R
import com.shishi.shishiface.faceserver.DataBase
import com.shishi.shishiface.log
import kotlinx.android.synthetic.main.fragment_about.*

class AboutFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private var clickTime = Array<Long>(5) { 0 }
    private var mainInfoToastTime = System.currentTimeMillis()
    private val db: DataBase
        get() = DataBase.localDataBase!!
    private val mainInfo: String
        get() = "感谢您使用ShishiFace！\n" +
                "PACKAGE NAME: com.shishi.shishiface\n" +
                "ShishiFace由四川省成都市石室中学STEAM实验室FTC项目队伍UST程序员Summer-lights设计\n\n" +
                "已安装的软件版本号: ${BuildConfig.VERSION_NAME}\n" +
                "ArcFace引擎版本: ${run {
                    val versionInfo = VersionInfo()
                    FaceEngine.getVersion(versionInfo)
                    versionInfo.version
                }}\n" +
                "用于多线程运算的手机CPU核心数: ${Config.listThreadNum}\n"
    private val eggsNum = Config.EGG_NUM
    private val developerInfo: String
        get() {
            val locked = db.updateEggs() == Config.EGG_NUM
            return if (locked) "[开发者信息]\n" +
                    "程序员：常霆钰\n" +
                    "QQ：1067088037\n" +
                    "电话：13618060211\n" +
                    "您已经解锁所有彩蛋！\n"
            else "[开发者信息]\n" +
                    "程序员：**钰\n" +
                    "QQ：106****037\n" +
                    "电话：136****0211\n" +
                    "解锁软件所有彩蛋才会显示隐藏部分\n"
        }
    private var hasLongClick = false

    private val versionName =
        arrayOf(
            "Beta 1.0",
            "Beta 1.1",
            "Beta 1.2",
            "Beta 1.3",
            "Beta 1.4",
            "Beta 1.5",
            "Beta 1.6",
            "Beta 1.7",
            "Beta 1.8",
            "Beta 1.9",
            "Beta 1.10",
            "Beta 1.11",
            "Beta 1.12",
            "Beta 1.13",
            "Beta 1.14",
            "Beta 1.15",
            "Beta 1.16",
            "2.0.0.190816_R",
            "2.0.1.190819_R",
            "2.0.2.190826_R"
        )
    private val versionInfo =
        arrayOf(
            "2019年6月23日发布\n" +
                    "首个完成基本功能的版本，可以执行人脸识别、录入新人脸、删除和导出工作等功能。\n" +
                    "还存在诸多问题没有解决，最显著的bug是当识别对象快速移动时程序会闪退。\n",
            "2019年6月23日发布\n" +
                    "修复识别对象快速移动时程序闪退的问题。\n",
            "2019年6月30日发布\n" +
                    "优化了部分提示界面的设计，例如：当人脸录入重复时以对话框的形式提醒而不是几秒就消失的横幅。\n" +
                    "完成了人脸导入功能，导入时加入人性化设计：导入人脸时会弹出对话框要求用户选择数据库文件中哪些需要导入到本地数据库，当要导入的文件中人脸与本地数据库中人脸重复时，会去除前面的勾选并增加提示语，方便用户确定是否导入重复。\n" +
                    "支持对人脸信息（ID和姓名）进行修改，在管理界面长按即可。\n",
            "2019年7月6日发布\n" +
                    "优化了管理现有人脸界面功能区的界面设计，将界面上多个按钮收起放置在由一个按钮触发的对话框里，使界面整体看起来更简洁。\n" +
                    "增加反选功能，方便对人脸进行管理。\n",
            "2019年7月13日发布\n" +
                    "增加了语音提醒功能。识别成功的声音来自Android原生铃声，识别失败的声音来自Windows错误提示音。\n",
            "2019年7月20日发布\n" +
                    "高二在校期间的最后一个版本。新增了闪光灯提醒功能：识别成功会亮起闪光灯，识别失败会闪烁闪光灯。对识别语音做了修改，在纯语音后面加了从微软电子音录制的中文提示语。\n",
            "2019年7月27日发布\n" +
                    "完成了从文件中录入功能，实现从照片批量录入人脸信息。使用4线程录入。\n",
            "2019年8月2日发布\n" +
                    "增加关于页面。从文件录入功能的录入线程数会根据手机CPU核心数自动调整。\n" +
                    "增加了账户管理功能，用户可以在这里修改系统管理员或增删管理员。\n" +
                    "管理现有人脸界面将“系统管理员”或“管理员”标签改为简写“S”或“A”，并修改排列顺序。\n" +
                    "修改人脸信息界面显示唯一ID。\n",
            "2019年8月3日发布\n" +
                    "增加重启引擎功能，引擎初始化位置修改以提高稳定性。\n" +
                    "强制锁定所有界面为竖屏防止部分机型因切换横屏而导致的异常。\n" +
                    "完成设置界面的功能。\n" +
                    "优化首次使用时体验。\n",
            "2019年8月4日发布\n" +
                    "遵循简洁唯美的原则，将填写人脸信息的界面从窗口式设计改为卡片式设计。\n",
            "2019年8月5日发布\n" +
                    "修复首次使用时激活出现的bug。\n" +
                    "填写信息时显示文本框的标题防止录入者忘记文本框含义。\n" +
                    "新增权限保留功能，识别成功后权限不会立刻注销而是根据使用者需要保留一段时间（具体时间可以在设置中修改）。\n" +
                    "清除所有数据处增加二次防护以防止手滑。\n" +
                    "可以根据需要选择性为录入人脸、验证权限和人脸识别分别开启活体识别功能。\n",
            "2019年8月6日发布\n" +
                    "修复从文件导入时按照标准命名文件出现的bug。\n" +
                    "重新设计活体识别需求弹窗，并对验证权限进行防权限丢失优化。\n" +
                    "新增识别方向设置功能，可以根据需求修改人脸识别方向。\n" +
                    "新增搜索人脸功能，提供关键字即可搜索对应的人脸。\n",
            "2019年8月9日发布\n" +
                    "新增系统管理员密码功能，防止暗光下解锁失败。\n" +
                    "将账户管理和关于的Activity修改成Fragment，以保证界面统一性。\n" +
                    "重新设计关于界面UI和响应模式。\n" +
                    "人脸列表和复选框增加动画。\n" +
                    "调整获取人脸信息弹窗的UI。\n" +
                    "调整管理人脸界面的UI。\n",
            "2019年8月12日发布\n" +
                    "修复了修改人脸信息时出现的bug。\n" +
                    "重新设计数据库组件，提高响应速度。\n" +
                    "增加了数据库加密功能，强制对本地数据库采用AES-256加密算法加密，为隐私数据保驾护航。\n",
            "2019年8月13日发布\n" +
                    "修复了账户管理界面的bug。\n" +
                    "通过增加密码位数来加强数据库加密的安全性。\n" +
                    "增加完整数据库导入导出的功能。\n",
            "2019年8月14日发布\n" +
                    "增加了人脸识别距离设置，动态调整远处人脸误识别界限，防止误识别。\n",
            "2019年8月16日发布\n" +
                    "增加splash界面，使加载界面更具趣味性，同时验证权限。\n" +
                    "管理人脸增加临时分组，帮助完成大批量人脸信息管理。\n" +
                    "优化关于界面的版本信息UI。\n",
            "2019年8月16日发布\n" +
                    "正式签名APK。\n",
            "2019年8月19日发布\n" +
                    "修复了权限保留时间出现的bug。\n",
            "2019年8月26日发布\n" +
                    "调整了更人性化的声音和闪光灯提醒方式。\n"
        )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        ustButton.setOnClickListener {
            Thread.sleep(10)
            if (System.currentTimeMillis() - mainInfoToastTime >= 2000 && !hasLongClick) {
                Toast.makeText(activity!!, "长按UST标徽即可显示软件信息", Toast.LENGTH_SHORT).show()
                mainInfoToastTime = System.currentTimeMillis()
            }
            for (i in clickTime.size - 1 downTo 1) {
                clickTime[i] = clickTime[i - 1]
            }
            clickTime[0] = System.currentTimeMillis()
            if (clickTime[0] - clickTime[clickTime.size - 1] <= 1500) {
                clickTime.forEachIndexed { index, _ -> clickTime[index] = 0 }//清除列表
                mainEgg()
            }
        }

        ustButton.setOnLongClickListener {
            hasLongClick = true
            mainInfo()
            false
        }

    }

    private fun mainEgg() {
        db.updateOrAddSettings(Config.EGG1, "1")
        AlertDialog.Builder(activity!!)
            .setTitle("彩蛋")
            .setMessage("恭喜您找了一个彩蛋！\n剩余${Config.EGG_NUM - db.updateEggs()}个，继续努力！")
            .setPositiveButton("好") { _, _ ->
                mainInfo()
            }
            .setCancelable(false)
            .show()
    }

    private fun mainInfo() {
        val dialog = AlertDialog.Builder(activity!!)
            .setTitle("关于")
        if (db.getSettings(Config.EGG1)?.value == "1") {
            dialog
                .setMessage("$mainInfo \n$developerInfo \n共${eggsNum}个彩蛋，已找到${db.updateEggs()}个")
                .setPositiveButton("版本信息") { _, _ -> version() }
                .setNegativeButton("关闭", null)
                .setNeutralButton("清空彩蛋") { _, _ -> clearEggs() }
        } else {
            dialog
                .setMessage("$mainInfo \n这是一个充满惊喜的世界，比如这个界面就拥有一个！请擦亮您善于发现的眼睛~")
                .setPositiveButton("版本信息") { _, _ -> version() }
                .setNegativeButton("关闭", null)
        }
        dialog.show()
    }

    private fun version() {

        fun showVersion(index: Int) {
            AlertDialog.Builder(activity!!)
                .setTitle(versionName[index])
                .setMessage(versionInfo[index])
                .setPositiveButton("返回") { _, _ -> version() }
                .show()
        }

        AlertDialog.Builder(activity!!)
            .setTitle("版本信息")
            .setItems(versionName) { _, which ->
                showVersion(which)
            }
            .setPositiveButton("返回") { _, _ -> mainInfo() }
            .show()
    }

    private fun clearEggs() {
        AlertDialog.Builder(activity!!)
            .setTitle("清空")
            .setMessage("您确定要清空已找到的彩蛋吗？\n此操作不可逆！")
            .setPositiveButton("确定") { _, _ ->
                for (i in Config.EGG_INDEX) {
                    db.deleteSettings(i)
                }
                Toast.makeText(activity!!, "已清空彩蛋！", Toast.LENGTH_SHORT).show()
                hasLongClick = false
                mainInfo()
            }
            .setNegativeButton("取消") { _, _ -> mainInfo() }
            .show()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        hasLongClick = false
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