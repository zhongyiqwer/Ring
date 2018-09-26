package com.example.ring

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
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
import com.example.ring.util.SettingUtils
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() ,View.OnClickListener{

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
        val intent = Intent(this, LongConnService::class.java)
        startService(intent)
    }

    private fun initView() {
        broadcast = MyConnectStateBroadcast()
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.example.ring.connectSuccess")
        intentFilter.addAction("com.example.ring.disConnected")
        registerReceiver(broadcast,intentFilter)

        btn_scan.setOnClickListener(this)
        adapter = DeviceAdapter(this)
        list_device.adapter = adapter
        operatingAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.rotate)
        operatingAnim.setInterpolator(LinearInterpolator())

        adapter.setOnDeviceClickListener(object: DeviceAdapter.OnDeviceClickListener {
            override fun onSelectPhone() {
                Toast.makeText(this@MainActivity,"请先输出紧急联系人电话号码",Toast.LENGTH_SHORT).show()
            }

            override fun onConnect(bleDevice: BleDevice?) {
                if (!BleManager.getInstance().isConnected(bleDevice) && bleDevice!!.mac.isNotEmpty()) {
                    BleManager.getInstance().cancelScan()
                    val edit = getSharedPreferences("Ble", 0).edit()
                    edit.putString("bleDeviceMac",bleDevice.mac)
                    edit.commit()
                    startMyService()
                }
            }
            override fun onDisConnect(bleDevice: BleDevice?) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().disconnect(bleDevice)
                }
            }
        })

        btnSettingPhone.setOnClickListener (this)
        btnSettingAuto.setOnClickListener(this)
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

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
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
        unregisterReceiver(broadcast)
    }
}
