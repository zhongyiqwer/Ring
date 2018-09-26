package com.example.ring.manager

import android.content.Context
import android.content.Intent
import com.example.ring.KeepLiveActivity

/**
 * Created by ZY on 2018/9/25.
 */
object KeepLiveManager {
    var keepLiveActivity:KeepLiveActivity?=null

    fun startKeepLiveActivity(context: Context){
        println("创建KeepLive")
        val intent = Intent(context, KeepLiveActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun finishKeepLiveActivity(){
        println("finish KeepLive")
        keepLiveActivity?.finish()
    }
}