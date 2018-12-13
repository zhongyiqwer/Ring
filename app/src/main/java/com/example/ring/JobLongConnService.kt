package com.example.ring

import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Build
import android.support.annotation.RequiresApi
import com.clj.fastble.BleManager
import com.example.ring.manager.KeepLiveManager
import com.example.ring.util.LogUtil
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
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction("com.example.ring.destroy.LonConnService")
        registerReceiver(myServiceBroadcast, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("start$TAG")
        LogUtil.d(TAG,"Job onStartCommand")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                println("走7.0间隔")
                //因为7.0以上系统最小间隔为15分钟，所以用2个job互相定时开启的方式来实现
                JobInfo.Builder(JobLongConnService.jobId, ComponentName(applicationContext, JobLongConnService::class.java))
                        .setMinimumLatency(30*1000)
                        .setOverrideDeadline(40*1000)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setPersisted(true)
            } else {
                println("走30秒间隔")
                JobInfo.Builder(JobLongConnService.jobId, ComponentName(applicationContext, JobLongConnService::class.java))
                        .setPeriodic(30 * 1000)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setPersisted(true)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                        .setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR)
            }
            if (jobScheduler.schedule(builder.build()) == JobScheduler.RESULT_SUCCESS) {
                println("工作成功")
            } else {
                println("工作失败")
            }
        }
        return Service.START_STICKY
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        println("job启动$TAG")
        LogUtil.d(TAG,"job启动")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            println("job 7.0 启动$TAG")
            LogUtil.d(TAG,"job 7.0 启动")
            startLonConnJob()
            scheduleRefresh()
            jobFinished(params,false)
            return true
        }else{
           startLonConnJob()
           jobFinished(params,false)
        }
        return false
    }

    private fun scheduleRefresh() {
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val builder = JobInfo.Builder(jobId, ComponentName(applicationContext, JobLongConnService::class.java))
                .setMinimumLatency(30 * 1000)
                .setPeriodic(15*60*1000)
                .setOverrideDeadline(40*1000)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setPersisted(true)
        if (jobScheduler.schedule(builder.build()) == JobScheduler.RESULT_SUCCESS) {
            println("工作7.0成功$TAG")
        } else {
            println("工作7.0失败$TAG")
        }
    }

    private fun startLonConnJob() {
        if (!ServiceUtils.isServiceRunning(this, "com.example.ring.LongConnService")) {
            LogUtil.d(TAG,"LongService is live")
            val serviceIntent = Intent(this, LongConnService::class.java)
            this.startService(serviceIntent)
        } else {
            println("bleSize=${BleManager.getInstance().allConnectedDevice.size}")
            val bleDeviceMac = getSharedPreferences("Ble", 0).getString("bleDeviceMac", "")
            println("isConnected=${BleManager.getInstance().isConnected(bleDeviceMac)}")
            LogUtil.d(TAG,"bleSize=${BleManager.getInstance().allConnectedDevice.size} isConnected=${BleManager.getInstance().isConnected(bleDeviceMac)}")
            if (BleManager.getInstance().allConnectedDevice.size == 0 || !BleManager.getInstance().isConnected(bleDeviceMac)) {
                val serviceIntent = Intent(this, LongConnService::class.java)
                this.startService(serviceIntent)
            }
        }
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        println("job停止$TAG")
        LogUtil.d(TAG,"job停止")
        startLonConnJob()
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d(TAG,"job Destroy")
        println("$TAG Destroy")
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
                /*"com.example.ring.disConnected"->{
                    LogUtil.d(TAG,"JobService disConnected")
                    println("$TAG JobService disConnected")
                    stopSelf()
                }*/
                "com.example.ring.destroy.LonConnService"->{
                    //stopSelf()
                    println("longConnService服务挂了...")
                    startLonConnJob()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED ->{
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when(state){
                        BluetoothAdapter.STATE_OFF->{
                            LogUtil.d("JobLongConnService","STATE_OFF")
                            println("$TAG STATE_OFF")
                            //stopSelf()
                            startLonConnJob()
                        }
                        BluetoothAdapter.STATE_ON->{
                            LogUtil.d("JobLongConnService","STATE_ON")
                            println("$TAG STATE_ON")
                            //stopSelf()
                            startLonConnJob()
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED->{
                    LogUtil.d("JobLongConnService","ACTION_ACL_DISCONNECTED")
                    println("$TAG ACTION_ACL_DISCONNECTED")
                    //stopSelf()
                    startLonConnJob()
                }
                BluetoothDevice.ACTION_ACL_CONNECTED->{
                    LogUtil.d("JobLongConnService","ACTION_ACL_CONNECTED")
                    println("$TAG ACTION_ACL_CONNECTED")
                    //stopSelf()
                    startLonConnJob()
                }
            }
        }
    }
}