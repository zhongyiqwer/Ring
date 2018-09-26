package com.example.ring.util

import android.app.ActivityManager
import android.content.Context

/**
 * Created by ZY on 2018/9/24.
 */
object ServiceUtils {
    fun isServiceRunning(context: Context,serviceName:String):Boolean{
        if (serviceName.isNotEmpty()){
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = manager.getRunningServices(Int.MAX_VALUE)
            for (i in runningServices.indices){
                if (runningServices[i].service.className.toString()==serviceName){
                    return true
                }
            }

        }
        return false
    }
}