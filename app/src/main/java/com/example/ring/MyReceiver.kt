package com.example.ring

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ring.util.LogUtil
import com.example.ring.util.ServiceUtils

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        println("广播："+intent.action)
        when (intent.action){
            "com.example.ring.destroy" -> {
                println("jobLongService服务挂了...")
                checkRunning(context)
            }
            Intent.ACTION_BOOT_COMPLETED->{
                println("手机开机了...")
                checkRunning(context)
            }
            Intent.ACTION_USER_PRESENT->{
                println("手机解锁了...")
                checkRunning(context)
            }
            "cn.jpush.android.intent.DaemonService"->{
                println("极光推送拉起...")
                checkRunning(context)
            }
            /*BluetoothAdapter.ACTION_STATE_CHANGED ->{
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when(state){
                    BluetoothAdapter.STATE_OFF->{
                        LogUtil.d("JobLongConnService","STATE_OFF")
                        stopJobService(context)
                    }
                    BluetoothAdapter.STATE_ON->{
                        LogUtil.d("JobLongConnService","STATE_ON")
                        stopJobService(context)
                    }
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED->{
                LogUtil.d("JobLongConnService","ACTION_ACL_DISCONNECTED")
                stopJobService(context)
            }
            BluetoothDevice.ACTION_ACL_CONNECTED->{
                LogUtil.d("JobLongConnService","ACTION_ACL_CONNECTED")
                stopJobService(context)
            }*/
        }
    }

    private fun stopJobService(context:Context) {
        println("stopJobService")
        val intent = Intent(context, JobLongConnService::class.java)
        context.stopService(intent)
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
