package com.example.ring

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.Toast
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.example.massor.adapter.DeviceAdapter
import com.example.ring.util.ServiceUtils
import com.example.ring.util.SettingUtils
import com.example.ring.util.Utils
import kotlinx.android.synthetic.main.acc_con_temp_layout.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() ,View.OnClickListener{
    private val TAG = "MainActivity"
    lateinit var conn :ServiceConnection
    lateinit var adapter: DeviceAdapter
    lateinit var operatingAnim: Animation
    lateinit var longConnService: LongConnService
    lateinit var broadcast: MyConnectStateBroadcast

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            startJobService()
        }else {
            startMyService()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startJobService() {
        val intent = Intent(this, JobLongConnService::class.java)
        startService(intent)
        //因为Job是start后不直接启动，是到时间后启动
        startMyService()
    }

   /* private fun startAndBindMyService() {
        conn = MyConnection()
        val intent = Intent(this, LongConnService::class.java)
        bindService(intent,conn, Context.BIND_AUTO_CREATE)
        startService(intent)
    }*/

    private fun startMyService(){
        if (!ServiceUtils.isServiceRunning(this,"com.example.ring.LongConnService")){
            val serviceIntent = Intent(this, LongConnService::class.java)
            startService(serviceIntent)
        }
    }

    private fun initView() {
        if (!Utils.isNetworkAvailable(this)){
            Toast.makeText(this@MainActivity,"请开启网络",Toast.LENGTH_SHORT).show()
        }

        broadcast = MyConnectStateBroadcast()
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.example.ring.connectSuccess")
        intentFilter.addAction("com.example.ring.disConnected")
        intentFilter.addAction("com.example.ring.msgContent")
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast,intentFilter)

        btn_scan.setOnClickListener(this)
        adapter = DeviceAdapter(this)
        list_device.adapter = adapter
        operatingAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.rotate)
        operatingAnim.setInterpolator(LinearInterpolator())

        adapter.setOnDeviceClickListener(object: DeviceAdapter.OnDeviceClickListener {
            override fun onSelectPhone() {
                Toast.makeText(this@MainActivity,"请先输出紧急联系人电话号码",Toast.LENGTH_SHORT).show()
            }

            override fun onConnect(bleDevice: BleDevice) {
                if (!BleManager.getInstance().isConnected(bleDevice) && bleDevice.mac.isNotEmpty()) {
                    BleManager.getInstance().cancelScan()
                    val edit = getSharedPreferences("Ble", 0).edit()
                    edit.putString("bleDeviceMac",bleDevice.mac)
                    edit.commit()
                    /*if (bleDevice.device.bondState == BluetoothDevice.BOND_NONE){
                        val bond = bleDevice.device.createBond()
                        println("bond = $bond")
                    }*/
                    startMyService()
                }
            }
            override fun onDisConnect(bleDevice: BleDevice) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().disconnect(bleDevice)
                }
            }
        })

        btnSettingPhone.setOnClickListener (this)
        btnSettingAuto.setOnClickListener(this)
        btnCanCall.setOnClickListener(this)
        btnSendMsg.setOnClickListener(this)
        btnCleanCount.setOnClickListener(this)

        val preferences = getSharedPreferences("Ble", 0)
        val canCall = preferences.getBoolean("canCall", false)
        val sendMsg = preferences.getBoolean("sendMsg", false)
        if (!canCall){
            btnCanCall.text = "开启拨号"
        }else{
            btnCanCall.text = "关闭拨号"
        }

        if (!sendMsg){
            btnSendMsg.text = "开启短信"
        }else{
            btnSendMsg.text = "关闭短信"
        }
    }


    /*inner class MyConnection :ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            println("onServiceDisconnected")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            println("onServiceConnected")
            val localBinder = service as LocalBinder
            longConnService = localBinder.getService()
            println("MainActivity蓝牙："+BleManager.getInstance().isBlueEnable)
            val preferences = getSharedPreferences("Ble", 0)
            val bleDeviceMac = preferences.getString("bleDeviceMac", "")
            //if (bleDeviceMac.isEmpty()){
                startScan()
            //}
        }
    }*/

    inner class MyConnectStateBroadcast : BroadcastReceiver(){

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                "com.example.ring.msgContent"->{
                    val isAcc = intent.getBooleanExtra("isAcc", false)
                    if (isAcc){
                        val intArrayExtra = intent.getIntArrayExtra("intArray")
                        if (intArrayExtra[0]>0){
                            imTop.background=getDrawable(R.color.green)
                            imBottom.background=getDrawable(R.color.normoal)
                        }else{
                            imTop.background=getDrawable(R.color.normoal)
                            imBottom.background=getDrawable(R.color.green)
                        }

                        if (intArrayExtra[1]>0){
                            imLeft.background=getDrawable(R.color.green)
                            imRight.background=getDrawable(R.color.normoal)
                        }else{
                            imLeft.background=getDrawable(R.color.normoal)
                            imRight.background=getDrawable(R.color.green)
                        }

                        if (intArrayExtra[2]>0){
                            imCentener.background=getDrawable(R.color.green)
                        }else{
                            imCentener.background=getDrawable(R.color.yellow)
                        }

                    }else{
                        val msgContent = intent.getStringExtra("msgContent")
                        Toast.makeText(this@MainActivity,"$msgContent",Toast.LENGTH_LONG).show()
                    }
                }
                "com.example.ring.connectSuccess" ->{
                    println("com.example.ring.connectSuccess")
                    val bundle = intent.getBundleExtra("bundle")
                    val bleDevice = bundle["bleDevice"] as BleDevice
                    adapter.addDevice(bleDevice)
                    adapter.notifyDataSetChanged()
                }
                "com.example.ring.disConnected" ->{
                    println("com.example.ring.disConnected")
                    val bundle = intent.getBundleExtra("bundle")
                    val bleDevice = bundle["bleDevice"] as BleDevice
                    adapter.removeDevice(bleDevice)
                    adapter.notifyDataSetChanged()
                    imCentener.background=getDrawable(R.color.normoal)
                    imTop.background=getDrawable(R.color.normoal)
                    imBottom.background=getDrawable(R.color.normoal)
                    imLeft.background=getDrawable(R.color.normoal)
                    imRight.background=getDrawable(R.color.normoal)
                }
                "com.example.ring.connectFail" ->{
                    println("com.example.ring.connectFail")
                    //val bundle = intent.getBundleExtra("bundle")
                    //val bleDevice = bundle["bleDevice"] as BleDevice
                    Toast.makeText(this@MainActivity,"连接失败",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startScan() {
        while(true){
            if (BleManager.getInstance().isBlueEnable){
                break
            }else{
                BleManager.getInstance().enableBluetooth()
            }
        }
        BleManager.getInstance().scan(object: BleScanCallback(){
            override fun onScanFinished(p0: MutableList<BleDevice>?) {
                println("扫描结束")
                img_loading.clearAnimation()
                img_loading.visibility = View.INVISIBLE
                btn_scan.text = getString(R.string.start_scan)
            }
            override fun onScanStarted(p0: Boolean) {
                println("开始扫描")
                adapter.clearScanDevice()
                val allConnectedDevice = BleManager.getInstance().allConnectedDevice
                for (bleDevice in allConnectedDevice){
                    adapter.addDevice(bleDevice)
                }
                adapter.notifyDataSetChanged()
                img_loading.startAnimation(operatingAnim)
                img_loading.visibility = View.VISIBLE
                btn_scan.text = getString(R.string.stop_scan)
            }
            override fun onScanning(bleDevice: BleDevice?) {
                println("扫描到设备:"+bleDevice!!.mac.toString())
                adapter.addDevice(bleDevice!!)
                adapter.notifyDataSetChanged()
            }
        })
    }

    override fun onClick(v: View?) {
        when (v) {
            btn_scan -> {
                if (btn_scan.text == getString(R.string.start_scan)) {
                    startScan()
                } else if (btn_scan.text == getString(R.string.stop_scan)) {
                    BleManager.getInstance().cancelScan()
                }
            }
            btnSettingPhone->{
                val preferences = getSharedPreferences("Ble", 0)
                val oldPhone = preferences.getString("phone", "")
                val editText = EditText(this)
                editText.inputType = InputType.TYPE_CLASS_PHONE
                editText.setText(oldPhone)
                AlertDialog.Builder(this)
                        .setTitle("请输入电话号码")
                        .setPositiveButton("确定") { dialog, which ->
                            if (editText.text.toString().length==11){
                                val phone = editText.text.toString()
                                preferences.edit().putString("phone",phone).commit()
                            }
                        }.setView(editText)
                        .show()
            }
            btnSettingAuto->{
                SettingUtils.enterWhiteListSetting(this)
            }
            btnCanCall->{
                val preferences = getSharedPreferences("Ble", 0)
                val phone = preferences.getString("phone", "")
                if (btnCanCall.text.toString()=="关闭拨号"){
                    preferences.edit().putBoolean("canCall", false).commit()
                    btnCanCall.text = "开启拨号"
                }else{
                    if (phone.isEmpty()){
                        Toast.makeText(this,"请输入手机号",Toast.LENGTH_SHORT).show()
                        return
                    }
                    preferences.edit().putBoolean("canCall", true).commit()
                    btnCanCall.text = "关闭拨号"
                }
            }
            btnSendMsg->{
                val preferences = getSharedPreferences("Ble", 0)
                val phone = preferences.getString("phone", "")
                if (btnSendMsg.text.toString()=="关闭短信"){
                    preferences.edit().putBoolean("sendMsg", false).commit()
                    btnSendMsg.text = "开启短信"
                }else{
                    if (phone.isEmpty()){
                        Toast.makeText(this,"请输入手机号",Toast.LENGTH_SHORT).show()
                        return
                    }
                    preferences.edit().putBoolean("sendMsg", true).commit()
                    btnSendMsg.text = "关闭短信"
                }
            }
            btnCleanCount->{
                val preferences = getSharedPreferences("Ble", 0)
                val edit = preferences.edit()
                val mutableMap = preferences.all
                for (i in mutableMap.keys){
                    if (i.startsWith("last") && i.endsWith("Time")){
                        edit.putLong(i,0)
                    }else if ("firstTime" == i) {
                        edit.putLong(i,0)
                    }else if ("orderCount" == i){
                        edit.putInt(i,0)
                    }
                }
                edit.commit()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        //当按下回退键时不杀掉应用而是回到桌面使应用变为后台运行
        if(keyCode==KeyEvent.KEYCODE_BACK){
            moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        //unbindService(conn)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast)
    }
}
