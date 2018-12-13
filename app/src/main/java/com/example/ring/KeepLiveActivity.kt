package com.example.ring

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import com.example.ring.manager.KeepLiveManager

/**
 * Created by ZY on 2018/9/25.
 */
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
        regist()
    }

    private fun regist() {
        broadcast = MyBroadcast()
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.example.ring.callPhone")
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast,intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast)
    }

    inner class MyBroadcast:BroadcastReceiver(){
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            when(intent.action){
                "com.example.ring.callPhone"->{
                    println("com.example.ring.callPhone")
                    val phone = getSharedPreferences("Ble", 0).getString("phone", "")
                    val uri = Uri.parse("tel:$phone")
                    val intent = Intent(Intent.ACTION_CALL, uri)
                    startActivity(intent)
                }
            }
        }
    }
}

