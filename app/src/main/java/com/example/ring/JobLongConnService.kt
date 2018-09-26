package com.example.ring

import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.*
import android.os.Build
import android.support.annotation.RequiresApi
import com.clj.fastble.BleManager
import com.example.ring.manager.KeepLiveManager
import com.example.ring.util.ServiceUtils

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
/**
 * Created by ZY on 2018/9/25.
 */
class JobLongConnService:JobService() {

    companion object {
        private const val TAG = "JobLongConnService:"
        const val jobId = 1
    }

    lateinit var myServiceBroadcast: MyServiceBroadcast

    override fun onCreate() {
        super.onCreate()
        myServiceBroadcast = MyServiceBroadcast()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        registerReceiver(myServiceBroadcast, intentFilter)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("start$TAG")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                println("走15分钟间隔$TAG")
                JobInfo.Builder(jobId, ComponentName(applicationContext, JobLongConnService::class.java))
                        .setPeriodic(15 * 60 * 1000, 5 * 60 * 1000)
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .setPersisted(true)
            } else {
                println("走2分钟间隔$TAG")
                JobInfo.Builder(jobId, ComponentName(applicationContext, JobLongConnService::class.java))
                        .setPeriodic(30 * 1000)
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .setPersisted(true)
            }
            if (jobScheduler.schedule(builder.build()) == JobScheduler.RESULT_SUCCESS) {
                println("工作成功$TAG")
            } else {
                println("工作失败$TAG")
            }
        }
        return Service.START_STICKY
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        println("job启动$TAG")
        if (!ServiceUtils.isServiceRunning(this, "com.example.ring.LongConnService")) {
            val serviceIntent = Intent(this, LongConnService::class.java)
            this.startService(serviceIntent)
        } else {
            if (BleManager.getInstance().allConnectedDevice.size == 0) {
                val serviceIntent = Intent(this, LongConnService::class.java)
                this.startService(serviceIntent)
            }
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        println("job停止$TAG")
        if (!ServiceUtils.isServiceRunning(this, "com.example.ring.LongConnService")) {
            val serviceIntent = Intent(this, LongConnService::class.java)
            this.startService(serviceIntent)
        } else {
            if (BleManager.getInstance().allConnectedDevice.size == 0) {
                val serviceIntent = Intent(this, LongConnService::class.java)
                this.startService(serviceIntent)
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent("com.example.ring.destroy")
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
        unregisterReceiver(myServiceBroadcast)
    }

    inner class MyServiceBroadcast : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    KeepLiveManager.startKeepLiveActivity(this@JobLongConnService)
                }
                Intent.ACTION_SCREEN_ON -> {
                    KeepLiveManager.finishKeepLiveActivity()
                }
            }
        }
    }
}