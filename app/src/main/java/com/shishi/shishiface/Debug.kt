package com.shishi.shishiface

import android.util.Log

var lastTime = System.currentTimeMillis()

/**
 * 输入index后在logcat中即可查看每一段代码所需时间
 */
fun logTime(index: Int) {
    val time = System.currentTimeMillis() - lastTime
    Log.d("石室", "编号#$index   时间:$time")
    lastTime = System.currentTimeMillis()
}

/**
 * 比较省事的打印logcat
 */
fun log(any: Any) = Log.d("石室", any.toString())