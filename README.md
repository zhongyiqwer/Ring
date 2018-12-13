最近在做一个需要进程保活的项目，所以来总结一下。

这里给出了我的进程保活方法和一些技巧，最后附源码

主要步骤如下：

总体来说就是5.0以下开启定时器来启动Service，5.0以上开启JobService来开启Service。
然后监听系统的锁屏和解锁广播，来开启一个1像素的Activity。
监听系统的开机广播或第三方推送广播来启动Service。
对与我们的Service开启一个通知栏通知提升为前台进程，并使其为无感知。
引导用户开启应用自启动来使app在被手动或自动杀死后能从新被拉起来。

下面进行详细的描述：

 1. 5.0以下开启定时器来启动Service，5.0以上开启JobService来开启Service。

       5.0一下在Application中开启定时器

private fun startLongConn() {
        val systemService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, LongConnService::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val realtime = SystemClock.elapsedRealtime()
        systemService.setRepeating(AlarmManager.RTC_WAKEUP,realtime,60*1000,pendingIntent)
    }

    5.0以上使用JobService，并在其中接收屏幕解锁和锁屏的广播，来实现1像素Activity的开关，JobService的作用简单来说就是开启一个可以在5.0以上使用的定时器，在间隔时间执行一次onStartJob方法。但是当被用户杀死或在系统内存严重不足时也会被杀死，被系统标记为stopped，从而无法启动。（解决办法看下面的自启动）

      ps：锁屏和解锁广播要动态注册，静态注册无效。若不清楚JobService的用法请自行百度

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
                println("7.0走15分钟间隔$TAG")
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

   对于我们自己的Service为了实现不轻易被杀死，我们开启为前台进程，提高优先级，在开启无感知

class LongConnService: Service() {
   
    private val TAG = "LongConnService"


    inner class LocalBinder :Binder(){
        fun getService():LongConnService {
            return LongConnService()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        println("$TAG 返回binder")
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        println("$TAG 返回onUnbind")
        return super.onUnbind(intent)
    }

    //第一次创建的时候调用
    override fun onCreate() {
        super.onCreate()
        println("$TAG onCreate")
    }

    //每次startService的时候调用
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("$TAG onStartCommand")
        if (Build.VERSION.SDK_INT<18){
            this.startForeground(1000,  getNotification())
        }else if (Build.VERSION.SDK_INT<25){
            this.startForeground(1000,  getNotification())
            this.startService(android.content.Intent(this,InnerService::class.java))
        }else{
            this.startForeground(1000,  getNotification())
        }

        //做自己的处理

        return START_STICKY
    }

   
    override fun onDestroy() {
        println("$TAG onDestroy")
        stopForeground(true)
        val intent = Intent("com.example.ring.destroy")
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
        super.onDestroy()
    }

    private fun getNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, 0)
        return NotificationCompat.Builder(this)
                .setContentTitle("Ring")
                .setContentText("Ring service is running")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .setAutoCancel(false)
                .build()
    }

    //是为了让用户无感知
    companion object {
         class InnerService:Service(){
            override fun onBind(intent: Intent?): IBinder? {
                return null
            }

            override fun onCreate() {
                super.onCreate()
                try {
                    println("开启内部InnerService")
                    val intent = Intent(this, MainActivity::class.java)
                    val pi = PendingIntent.getActivity(this, 0, intent, 0)
                    val notification = NotificationCompat.Builder(this)
                            .setContentTitle("Ring")
                            .setContentText("Ring service is running")
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(R.drawable.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                            .setContentIntent(pi)
                            .setAutoCancel(false)
                            .build()
                    startForeground(1000,notification)
                }catch (throwable:Throwable){
                    throwable.printStackTrace()
                }
                stopSelf()
            }
        }
    }

}

  1像素Activity和其manager代码

class KeepLiveActivity:AppCompatActivity() {

    lateinit var broadcast:MyBroadcast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeepLiveManager.keepLiveActivity = this
        window.setGravity(Gravity.START)
        window.attributes.run {
            x=0
            y=0
            width=1
            height=1
        }
    }

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

另外在创建一个BroadcastReceiver来接收系统注册静态广播或第三方广播，来启动Service

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

最后为了在app被手动kill掉之后还能起来，我们需要引导用户开启自启动

object SettingUtils {

    fun enterWhiteListSetting(context: Context) {
        try {
            context.startActivity(getSettingIntent())
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun getSettingIntent(): Intent {

        var componentName: ComponentName? = null

        val brand = android.os.Build.BRAND

        when (brand.toLowerCase()) {
            "samsung" -> componentName = ComponentName("com.samsung.android.sm",
                    "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity")
            "huawei" -> componentName = ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            "xiaomi" -> componentName = ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity")
            "vivo" -> componentName = ComponentName("com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            "oppo" -> componentName = ComponentName("com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
            "360" -> componentName = ComponentName("com.yulong.android.coolsafe",
                    "com.yulong.android.coolsafe.ui.activity.autorun.AutoRunListActivity")
            "meizu" -> componentName = ComponentName("com.meizu.safe",
                    "com.meizu.safe.permission.SmartBGActivity")
            "oneplus" -> componentName = ComponentName("com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
            "nubia"->{
                componentName = ComponentName("cn.nubia.security2",
                        "cn.nubia.security.appmanage.selfstart.ui.SelfStartActivity")
            }
            else -> {
            }
        }

        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (componentName != null) {
            intent.component = componentName
        } else {
            intent.action = Settings.ACTION_SETTINGS
        }
        return intent
    }
}

然后你就会发现若是不关闭自启动，app是杀不死的，kill了之后一会儿又会起来。（你们看微信也是开启了自启动）

上面开启自启动的适合大部分机型，少部分没有的可以自己添加，手机进入自启动界面，然后使用adb命令获取路径

完整代码已上传至github：https://github.com/zhongyiqwer/Ring

