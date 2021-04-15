package com.shishi.shishiface.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.arcsoft.face.*
import com.shishi.shishiface.Config
import com.shishi.shishiface.R
import com.shishi.shishiface.faceserver.DataBase
import com.shishi.shishiface.fragment.*
import com.shishi.shishiface.log
import kotlinx.android.synthetic.main.fragment_manage_face.*
import net.sqlcipher.database.SQLiteDatabase
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, MainFragment.OnFragmentInteractionListener,
    BlankFragment.OnFragmentInteractionListener, ManageFaceFragment.OnFragmentInteractionListener, AccountFragment.OnFragmentInteractionListener,
    AboutFragment.OnFragmentInteractionListener {

    //Activity
    private lateinit var fragmentManager: androidx.fragment.app.FragmentManager
    private val fragment: Int = R.id.fragment
    private val ACTION_REQUEST_PERMISSIONS = 0x001
    private lateinit var db: DataBase

    private lateinit var mainFragment: MainFragment
    private lateinit var manageFaceFragment: ManageFaceFragment
    private lateinit var accountFragment: AccountFragment
    private lateinit var aboutFragment: AboutFragment
    private lateinit var navView: NavigationView

    private var state = Config.UNKNOWN_STATE
    private var onWhichItem = OnWhichItem.Main
    enum class OnWhichItem {
        Main, Recognition, ManageFace, Account, About, Unknown
    }
    private var lastBackPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction().replace(fragment, BlankFragment()).commit()

        val toolbar: Toolbar = findViewById(R.id.accountToolbar)
        toolbar.title = "成都石室中学人脸识别系统"
        setSupportActionBar(toolbar)

        SQLiteDatabase.loadLibs(this)//加载so文件
        SQLiteDatabase.openOrCreateDatabase(Config.localDBPath, "", null)//无密码登录
        Config.imei = Config.getIMEI(this, 0)
        Config.localDBPath = getExternalFilesDir("")!!.path + "/face.db"
        db = DataBase.init(Config.localDBPath, true)
        DataBase.localDataBase = db

        mainFragment = MainFragment()
        manageFaceFragment = ManageFaceFragment()
        accountFragment = AccountFragment()
        aboutFragment = AboutFragment()

        fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
            .add(fragment, mainFragment)
            .add(fragment, manageFaceFragment)
            .add(fragment, accountFragment)
            .add(fragment, aboutFragment)
            .commit()//载入
        hideAllFragment()//全部隐藏
        fragmentManager.beginTransaction().show(mainFragment).commit()//显示主界面

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)
        navView.setCheckedItem(R.id.nav_home)
        navView.menu.findItem(R.id.version).title = "版本号 - ${Config.VERSION}"

        Config.direction = db.getOrAddSettings(Config.RECOGNITION_DIRECTION, "3").value
        db.getFace().forEach {
            if (it.permission == Config.SYSTEM_ADMINISTRATOR) Config.firstUse = false
        }
        if (Config.firstUse) {
            startActivity(Intent(this, FirstUseActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (Config.state == Config.EXIT) {
            Config.state = Config.UNKNOWN_STATE
            finish()
            exitProcess(0)
        }
    }

    override fun onDestroy() {
        db.close()
        DataBase.localDataBase!!.close()
        Config.destroyEngineAndList()
        super.onDestroy()
    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    /**
     * 隐藏所有fragment
     */
    private fun hideAllFragment() {
        fragmentManager.beginTransaction()
            .hide(mainFragment)
            .hide(manageFaceFragment)
            .hide(accountFragment)
            .hide(aboutFragment)
            .commit()
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            if (onWhichItem != OnWhichItem.Main) {
                hideAllFragment()
                onWhichItem = OnWhichItem.Main
                fragmentManager.beginTransaction().show(mainFragment).commit()
                navView.setCheckedItem(R.id.nav_home)
                Toast.makeText(this, "再次按下返回键即可退出", Toast.LENGTH_SHORT).show()
            } else {
                if (System.currentTimeMillis() - lastBackPressedTime < 2000) {
                    finish()
                    overridePendingTransition(R.anim.activity_open, R.anim.activity_close)
                    exitProcess(0)
                } else {
                    Toast.makeText(this, "再次按下返回键即可退出", Toast.LENGTH_SHORT).show()
                }
            }
        }
        lastBackPressedTime = System.currentTimeMillis()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.active -> {
                activeFaceEngine()
                true
            }
            R.id.action_settings -> {
                if (Config.permissions >= Config.SYSTEM_ADMINISTRATOR) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                } else {
                    state = Config.SETTINGS
                    val intent = Intent(this, FaceRecognitionActivity::class.java)
                    intent.putExtra("state", Config.GET_PERMISSIONS)
                    startActivityForResult(intent, Config.GET_PERMISSIONS)
                }
                true
            }
            R.id.destroy_permission -> {
                Config.permissions = Config.GUEST
                Toast.makeText(this, "权限已注销", Toast.LENGTH_SHORT).show()
                toMainFragment()
                true
            }
            R.id.exit -> {
                finish()
                overridePendingTransition(R.anim.activity_open, R.anim.activity_close)
                exitProcess(0)
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        onWhichItem = when (item.itemId) {
            R.id.nav_home -> {
                hideAllFragment()
                fragmentManager.beginTransaction().show(mainFragment).commit()
                OnWhichItem.Main
            }
            R.id.recognition -> {
                val intent = Intent(this, FaceRecognitionActivity::class.java)
                intent.putExtra("state", Config.RECOGNITION)
                startActivityForResult(intent, Config.RECOGNITION)
                OnWhichItem.Recognition
            }
            R.id.manage_faces -> {
                if (onWhichItem == OnWhichItem.ManageFace) {
                    hideAllFragment()
                    fragmentManager.beginTransaction().show(mainFragment).commit()
                }
                if (Config.permissions >= Config.ADMINISTRATOR) {
                    toManageFaceFragment()
                } else {
                    val intent = Intent(this, FaceRecognitionActivity::class.java)
                    intent.putExtra("state", Config.GET_PERMISSIONS)
                    startActivityForResult(intent, Config.GET_PERMISSIONS)
                }
                OnWhichItem.ManageFace
            }
            R.id.account_management -> {
                if (Config.permissions >= Config.SYSTEM_ADMINISTRATOR) {
                    toAccountFragment()
                } else {
                    val intent = Intent(this, FaceRecognitionActivity::class.java)
                    intent.putExtra("state", Config.GET_PERMISSIONS)
                    startActivityForResult(intent, Config.GET_PERMISSIONS)
                }
                OnWhichItem.Account
            }
            R.id.about -> {
                hideAllFragment()
                fragmentManager.beginTransaction().show(aboutFragment).commit()
                OnWhichItem.About
            }
            else -> OnWhichItem.Main
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun toMainFragment() {
        runOnUiThread {
            hideAllFragment()
            fragmentManager.beginTransaction().show(mainFragment).commit()
            onWhichItem = OnWhichItem.Main
            Thread.sleep(10)
            navView.setCheckedItem(R.id.nav_home)
        }
    }

    private fun toManageFaceFragment() {
        val permission = Config.permissions
        if (permission >= Config.ADMINISTRATOR) {
            val text = "${getString(R.string.mf_text_view)}  ${Config.permissionToText(permission)}"
            manageFaceFragment.mf_text_view.text = text
            hideAllFragment()
            fragmentManager.beginTransaction().show(manageFaceFragment).commit()
        } else {
            toMainFragment()
            Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toAccountFragment() {
        val permission = Config.permissions
        if (permission >= Config.SYSTEM_ADMINISTRATOR) {
            hideAllFragment()
            fragmentManager.beginTransaction().show(accountFragment).commit()
        } else {
            toMainFragment()
            Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Config.RECOGNITION, Config.ABOUT, Config.ACCOUNT -> {
                toMainFragment()
            }
            Config.GET_PERMISSIONS -> {
                if (data == null) {
                    Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show()
                    toMainFragment()
                } else {
                    val permission = data.getIntExtra("permission", 0)
                    if (state == Config.UNKNOWN_STATE) {
                        when (onWhichItem) {
                            OnWhichItem.ManageFace -> toManageFaceFragment()
                            OnWhichItem.Account -> toAccountFragment()
                        }
                    } else {
                        when (state) {
                            Config.SETTINGS -> {
                                startActivity(Intent(this, SettingsActivity::class.java))
                            }
                        }
                    }
                }
            }
        }
        state = Config.UNKNOWN_STATE
    }

    /**
     * 激活软件
     */
    private fun activeFaceEngine(onCreate: Boolean = false) {
        val faceEngine = FaceEngine()
        val activeCode = FaceEngine.activeOnline(applicationContext,
            Config.APP_ID,
            Config.SDK_KEY
        )
        if (onCreate && activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
            //启动阶段重复激活不提示
        } else {
            AlertDialog.Builder(this)
                .setTitle("激活")
                .setMessage(when (activeCode) {
                    ErrorInfo.MOK -> {
                        getString(R.string.active_success)
                    }
                    ErrorInfo.MERR_ASF_ALREADY_ACTIVATED -> {
                        getString(R.string.already_activated)
                    }
                    else -> {
                        "${getString(R.string.active_failed)}  $activeCode"
                    }
                } + "\n感谢您使用基于ArcFace打造的ShishiFace")
                .setPositiveButton("好") { _, _ -> }
                .show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            var isAllGranted = true
            for (grantResult in grantResults) {
                isAllGranted = isAllGranted and (grantResult == PackageManager.PERMISSION_GRANTED)
            }
            if (!isAllGranted) {
                Toast.makeText(applicationContext, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                Thread {
                    Thread.sleep(2000)
                    finish()
                }.start()
            }
        }
    }

}
