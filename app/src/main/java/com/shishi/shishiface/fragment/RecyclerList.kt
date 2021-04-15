package com.shishi.shishiface.fragment

import com.shishi.shishiface.Config
import com.shishi.shishiface.faceserver.DataBase
import java.util.ArrayList

object RecyclerList {

    var faceItems: ArrayList<FaceItem> = ArrayList()
    var tempGroup: ArrayList<FaceItem> = ArrayList()

    //清空释放
    fun release() {
        faceItems = ArrayList()
    }

    fun addItem(faceItem: FaceItem) {
        faceItems.add(faceItem)
    }

    fun addItem(id: Int, sid: Int, name: String, gender: Int, permission: Int = Config.GUEST) {
        val item = FaceItem(
            id, sid, name, gender, permission
        )
        faceItems.add(item)
    }

    data class FaceItem(var id: Int, var sid: Int, var name: String, var gender: Int, var permission: Int) {
        override fun toString(): String = name
        var checked = false
        var isAdmin = permission >= Config.ADMINISTRATOR
    }

}
