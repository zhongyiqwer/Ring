package com.example.ring

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.baidu.mapapi.SDKInitializer

/**
 * Created by ZY on 2018/9/24.
 */
class MyApp:Application() {
    override fun onCreate() {
        super.onCreate()
        //百度地图
        println("app初始化")
        SDKInitializer.initialize(this)
        //5.0一下手机
        if (Build.VERSION.SDK_INT< Build.VERSION_CODES.LOLLIPOP) {
            startLongConn()
        }
    }

    //开始执行启动长连接服务
    private fun startLongConn() {
        val systemService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, LongConnService::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val realtime = SystemClock.elapsedRealtime()
        systemService.setRepeating(AlarmManager.RTC_WAKEUP,realtime,60*1000,pendingIntent)
    }
}