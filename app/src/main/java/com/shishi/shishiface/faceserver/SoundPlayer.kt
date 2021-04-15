package com.shishi.shishiface.faceserver

import android.content.Context
import android.media.MediaPlayer
import com.shishi.shishiface.R

class SoundPlayer(context: Context) {

    val success = MediaPlayer.create(context, R.raw.recognition_success)
    val successShort = MediaPlayer.create(context, R.raw.recognition_success_short)
    val fault = MediaPlayer.create(context, R.raw.recognition_fault)

    init {
        success.isLooping = false
        successShort.isLooping = false
        fault.isLooping = false
    }

    fun release() {
        success.release()
        successShort.release()
        fault.release()
    }

}