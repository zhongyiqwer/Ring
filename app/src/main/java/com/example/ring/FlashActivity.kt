package com.example.ring

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.widget.Toast
import com.example.ring.util.CheckPermissionUtils
import java.util.*

/**
 * Created by ZY on 2018/7/6.
 */
class FlashActivity : AppCompatActivity(){

    internal var intent : Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        setContentView(R.layout.activity_flash)
        intent = Intent(this, MainActivity::class.java)

        val timer = Timer()
        val task = object : TimerTask(){
            override fun run() {
                checkMyPermission()
            }

        }
        timer.schedule(task,1500)

    }

    private fun checkMyPermission() {
        val permissions = CheckPermissionUtils.checkPermission(this@FlashActivity)
        if (permissions.isEmpty()){
            startActivity(intent)
            finish()
        }else{
            ActivityCompat.requestPermissions(this@FlashActivity, permissions, 100);
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100){
            if (permissions.isNotEmpty()){
                for (grant in grantResults){
                    if (grant != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this,"拒绝权限无法使用部分功能",Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                startActivity(intent)
                finish()
            }
        }
    }
}