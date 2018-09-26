package com.example.ring

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.NotificationCompat
import android.telephony.SmsManager
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.utils.HexUtil
import com.example.ring.manager.KeepLiveManager
import java.util.*


/**
 * Created by ZY on 2018/6/30.
 */
class LongConnService: Service() {

    val writeUUID = "0000ffe9-0000-1000-8000-00805f9b34fb"
    val readUUID = "0000ffe4-0000-1000-8000-00805f9b34fb"

    private val TAG = "LongConnService"
    //val mBinder:IBinder = LocalBinder()
    lateinit var bleDevice : BleDevice
    lateinit var bleDeviceMac : String

    lateinit var writeGattCharacteristic: BluetoothGattCharacteristic
    lateinit var readGattCharacteristic: BluetoothGattCharacteristic

    lateinit var myLocation : String
    var latitude : Double?=0.0
    var longitude : Double?=0.0
    var phone:String=""

    lateinit var mLocationClient:LocationClient

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
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1,5000)
                .setSplitWriteNum(64)
                .setConnectOverTime(10000)
                .setOperateTimeout(5000)
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

        val preferences = getSharedPreferences("Ble", 0)
        bleDeviceMac = preferences.getString("bleDeviceMac", "")
        phone = preferences.getString("phone","")
        println("phone111=$phone")
        println("bleDeviceMac = $bleDeviceMac")

        BleManager.getInstance().init(application)

        while(true){
            if (BleManager.getInstance().isBlueEnable){
                break
            }else{
                BleManager.getInstance().enableBluetooth()
            }
        }

        if (bleDeviceMac.isNotEmpty()){
            connectBle(bleDeviceMac)
        }
        return START_STICKY
    }

    private fun connectBle(bleDeviceMac:String) {
        println("bleMac为空吗：$bleDeviceMac")
        if (bleDeviceMac.isNotEmpty() && !BleManager.getInstance().isConnected(bleDeviceMac)){

            BleManager.getInstance().connect(bleDeviceMac,object :BleGattCallback(){
                override fun onStartConnect() {
                    println("开始连接")
                }
                override fun onDisConnected(p0: Boolean, p1: BleDevice, p2: BluetoothGatt?, p3: Int) {
                    println("连接断开")
                    val intent = Intent()
                    intent.action = "com.example.ring.disConnected"
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    val bundle = Bundle()
                    bundle.putParcelable("bleDevice",p1)
                    intent.putExtra("bundle",bundle)
                    sendBroadcast(intent)
                }
                override fun onConnectSuccess(p0: BleDevice, p1: BluetoothGatt?, p2: Int) {
                    println("连接成功")
                    val intent = Intent()
                    intent.action = "com.example.ring.connectSuccess"
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    val bundle = Bundle()
                    bundle.putParcelable("bleDevice",p0)
                    intent.putExtra("bundle",bundle)
                    sendBroadcast(intent)
                    bleDevice = p0
                    connGatt()
                }
                override fun onConnectFail(p0: BleDevice, p1: BleException?) {
                    println("连接失败")
                    val intent = Intent()
                    intent.action = "com.example.ring.connectFail"
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    val bundle = Bundle()
                    bundle.putParcelable("bleDevice",p0)
                    intent.putExtra("bundle",bundle)
                    sendBroadcast(intent)
                }
            })
        }
    }

    fun connGatt() {
        var writeFlag = false
        var readFlag = false
        val gatt = BleManager.getInstance().getBluetoothGatt(bleDevice)
        val serviceList = ArrayList<BluetoothGattService>()
        for (service in gatt.services) {
            serviceList.add(service)
        }
        for (gattService in serviceList) {

            val characteristicList = ArrayList<BluetoothGattCharacteristic>()
            for (characteristic in gattService.characteristics) {
                characteristicList.add(characteristic)
            }
            for (gattCharacteristic in characteristicList) {
                if (gattCharacteristic.uuid.toString().equals(writeUUID)) {
                    writeGattCharacteristic = gattCharacteristic
                    println("write=" + writeGattCharacteristic.uuid.toString())
                    writeFlag = true
                }
                if (gattCharacteristic.uuid.toString().equals(readUUID)) {
                    readGattCharacteristic = gattCharacteristic
                    println("read=" + readGattCharacteristic.uuid.toString())
                    readFlag = true
                }
                if (writeFlag && readFlag) {
                    break
                }
            }
            if (writeFlag && readFlag) {
                starNotify()
                break
            }
        }
    }

    private fun starNotify() {
        BleManager.getInstance().notify(
                bleDevice, readGattCharacteristic.service.uuid.toString(),
                readGattCharacteristic.uuid.toString(),
                object : BleNotifyCallback() {
                    override fun onCharacteristicChanged(notify: ByteArray?) {
                        //一次通知结束符 0d0a
                        println("notify success: " + HexUtil.formatHexString(notify,true))
                        startSendMsg(notify!!)
                    }

                    override fun onNotifyFailure(p0: BleException?) {
                        println(p0.toString())
                    }

                    override fun onNotifySuccess() {
                        println("通知成功")
                    }
                }
        )
    }

    //收到手环求救信号，开始通过手机给联系人发短信打电话
    private fun startSendMsg(notify: ByteArray) {
        if (judgeMsg(notify)){
            getMyLocation()
            println("phone = $phone")
            if (phone.isNotEmpty()){
                callPhone()
            }
        }
    }

    private fun getMyLocation() {
        mLocationClient = LocationClient(this);     //声明LocationClient类
        mLocationClient.registerLocationListener(object : BDAbstractLocationListener(){
            override fun onReceiveLocation(location: BDLocation) {
                //详细地址
                myLocation = location.addrStr
                //维度
                latitude = location.latitude
                //经度
                longitude = location.longitude
                if (myLocation.isNotEmpty() && phone.isNotEmpty()){
                    sendMsg(phone)
                }
            }
        })
        val option = LocationClientOption()
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving)//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll")//可选，默认gcj02，设置返回的定位结果坐标系
        option.setIsNeedAddress(true)//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true)//可选，默认false,设置是否使用gps
        option.setEnableSimulateGps(false)//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        mLocationClient.setLocOption(option)
        //开启定位
        mLocationClient.start()
    }

    @SuppressLint("MissingPermission")
    private fun callPhone() {
        KeepLiveManager.startKeepLiveActivity(this)
        Thread(Runnable {
            Thread.sleep(1000)
            val intent = Intent()
            intent.action = "com.example.ring.callPhone"
            intent.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            sendBroadcast(intent)
            Thread.sleep(1000)
            KeepLiveManager.finishKeepLiveActivity()
        }).start()
    }

    private fun sendMsg(phone:String) {
        val smsManager = SmsManager.getDefault()
        val content = "我遇到麻烦了，请来帮助我。地点：${myLocation}，经度:${longitude}维度:$latitude"
        println("短信内容为：$content")
        val list = smsManager.divideMessage(content)
        for (text in list){
            //smsManager.sendTextMessage(phone,null,text,null,null)
        }
        mLocationClient.stop()
    }

    private fun judgeMsg(notify: ByteArray) :Boolean{
        //todo
        //val data = Protocol.getReceiveCmdData(notify)
        //println("数据为：$data")
        return true
    }



    override fun onDestroy() {
        println("$TAG onDestroy")
        stopForeground(true)
        val intent = Intent("com.example.ring.destroy")
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        sendBroadcast(intent)
        BleManager.getInstance().clearCharacterCallback(bleDevice)
        BleManager.getInstance().disconnectAllDevice()
        BleManager.getInstance().destroy()
        mLocationClient.stop()
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