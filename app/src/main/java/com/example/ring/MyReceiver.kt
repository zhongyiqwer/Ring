package com.example.ring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ring.util.ServiceUtils

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        println("广播："+intent.action)
        when {
            intent.action == "com.example.ring.destroy" -> {
                println("longService服务挂了...")
                checkRunning(context)
            }
            intent.action == Intent.ACTION_BOOT_COMPLETED->{
                println("手机开机了...")
                checkRunning(context)
            }
            Intent.ACTION_USER_PRESENT == intent.action->{
                println("手机解锁了...")
                checkRunning(context)
            }
            intent.action == "cn.jpush.android.intent.DaemonService"->{
                println("极光推送拉起...")
                checkRunning(context)
            }
        }
    }

    private fun checkRunning(context: Context){
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP){
            if (!ServiceUtils.isServiceRunning(context,"com.example.ring.JobLongConnService")){
                val serviceIntent = Intent(context, JobLongConnService::class.java)
                context.startService(serviceIntent)
            }
        }else{
            if (!ServiceUtils.isServiceRunning(context,"com.example.ring.LongConnService")){
                val serviceIntent = Intent(context, LongConnService::class.java)
                context.startService(serviceIntent)
            }
        }
    }
}
