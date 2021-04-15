package com.shishi.shishiface.faceserver

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.arcsoft.face.FaceFeature
import com.arcsoft.face.GenderInfo
import com.shishi.shishiface.Config
import com.shishi.shishiface.log
import net.sqlcipher.SQLException
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteException
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.NullPointerException

class DataBase private constructor(private val path: String, private var encryption: Boolean) {

    private var db: SQLiteDatabase = getDB()

    init {
        db.execSQL("create table if not exists face(_id integer primary key autoincrement, sid integer, name text, gender integer, permission integer, feature blob)")//人脸信息
        db.execSQL("create table if not exists settings(_id integer primary key autoincrement, item integer, value text)")//加密设置
    }

    private fun getDB(): SQLiteDatabase {
        val password = if (encryption) Config.defaultPassword else ""
        var turnOnEncryption = 0
        return try {
            //根据encryption自动调整
            turnOnEncryption = 0
            SQLiteDatabase.openOrCreateDatabase(path, password, null)//无密码登录
        } catch (e: SQLiteException) {
            try {
                turnOnEncryption = 1
                SQLiteDatabase.openOrCreateDatabase(path, Config.defaultPassword, null)//使用密码登录
            } catch (e: SQLiteException) {
                turnOnEncryption = if (encryption) 1 else 2
                File(path).delete()//实在打不开删了就好
                SQLiteDatabase.openOrCreateDatabase(path, password, null)//使用密码登录
            }
        } finally {
            encryption = when (turnOnEncryption) {
                0 -> encryption
                1 -> true
                2 -> false
                else -> true
            }
            //以下注释内容功能是在log中显示密码
//            val password1 = if (encryption) Config.defaultPassword else ""
//            val show = "${password1.subSequence(0, 9)}**********${password1.subSequence(19, password1.length)}"
//            log("加密密码：$show ，密码长度为:${password1.length}位")
        }
    }

    fun booleanToString(boolean: Boolean): String = if (boolean) "1" else "0"

    fun stringToBoolean(string: String): Boolean = when (string) {
        "1" -> true
        "0" -> false
        else -> TODO("错误参数")
    }

    fun close() {
        db.close()
    }

    fun addFace(faceInfo: FaceInfo) {
        if (!db.isOpen) return
        val cValue = ContentValues()
        cValue.put("sid", faceInfo.sid)
        cValue.put("name", faceInfo.name)
        cValue.put("gender", faceInfo.gender)
        cValue.put("permission", faceInfo.permission)
        cValue.put("feature", faceInfo.feature.featureData)
        if (!db.isOpen) return
        db.insert("face", null, cValue)
        cValue.clear()
    }

    fun addSettings(item: Int, value: String) {
        if (!db.isOpen) return
        val cValue = ContentValues()
        cValue.put("item", item)
        cValue.put("value", value)
        db.insert("settings", null, cValue)
        cValue.clear()
    }

    fun deleteFace(id: Int) {
        if (!db.isOpen) return
        db.delete("face", "_id=?", arrayOf(id.toString()))
    }

    fun deleteSettings(item: Int) {
        if (!db.isOpen) return
        db.delete("settings", "item=?", arrayOf(item.toString()))
    }

    fun updateFace(
        id: Int,
        sid: Int? = null,
        name: String? = null,
        gender: Int? = null,
        permission: Int? = null,
        feature: FaceFeature? = null
    ) {
        if (!db.isOpen) return
        val value = ContentValues()
        if (sid != null) value.put("sid", sid)
        if (name != null) value.put("name", name)
        if (gender != null) value.put("gender", gender)
        if (permission != null) value.put("permission", permission)
        if (feature != null) value.put("feature", feature.featureData)
        db.update("face", value, "_id=?", arrayOf(id.toString()))
        value.clear()
    }

    fun updateSettings(item: Int, value: String) {
        if (!db.isOpen) return
        val contentValues = ContentValues()
        contentValues.put("value", value)
        db.update("settings", contentValues, "item=?", arrayOf(item.toString()))
        contentValues.clear()
    }

    fun getFace(): ArrayList<FaceInfo> {
        if (!db.isOpen) return ArrayList()
        val faceInfo = ArrayList<FaceInfo>()
        val cursor = db.query("face", null, null, null, null, null, null)
        cursor.move(0)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("_id"))
            val sid = cursor.getInt(cursor.getColumnIndex("sid"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            val gender = cursor.getInt(cursor.getColumnIndex("gender"))
            val permission = cursor.getInt(cursor.getColumnIndex("permission"))
            val feature = FaceFeature(cursor.getBlob(cursor.getColumnIndex("feature")))
            faceInfo.add(FaceInfo(id, sid, name, gender, permission, feature))
        }//从数据库中读取数据
        cursor.close()//关闭
        return faceInfo
    }

    fun getFace(id: Int): FaceInfo? {
        for (i in getFace()) {
            if (id == i.id) return i
        }
        return null
    }

    fun getSettings(): ArrayList<SettingItem> {
        if (!db.isOpen) return ArrayList()
        val settingItem = ArrayList<SettingItem>()
        val cursor = db.query("settings", null, null, null, null, null, null)
        cursor.move(0)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("_id"))
            val item = cursor.getInt(cursor.getColumnIndex("item"))
            val value = cursor.getString(cursor.getColumnIndex("value"))
            settingItem.add(SettingItem(id, item, value))
        }//从数据库中读取数据
        cursor.close()//关闭
        return settingItem
    }

    fun getSettings(item: Int): SettingItem? {
        if (!db.isOpen) return null
        for (i in getSettings()) {
            if (item == i.item) return i
        }
        return null
    }

    fun updateOrAddSettings(item: Int, value: String) {
        if (getSettings(item) == null) addSettings(item, value)
        else updateSettings(item, value)
    }

    fun getOrAddSettings(item: Int, value: String): SettingItem {
        if (getSettings(item) == null) addSettings(item, value)
        return getSettings(item)!!
    }

    fun updateEggs(): Int {
        var num = 0
        for (i in Config.EGG_INDEX) {
            if (getOrAddSettings(i, "0").value == "1") num++
        }
        return num
    }

    data class FaceInfo(
        var id: Int,
        var sid: Int,
        var name: String,
        var gender: Int,
        var permission: Int,
        var feature: FaceFeature
    )

    data class SettingItem(
        var id: Int,
        var item: Int,
        var value: String
    )

    companion object {
        var localDataBase: DataBase? = null
        fun init(path: String, encryption: Boolean): DataBase {
            return DataBase(path, encryption)
        }
    }

}