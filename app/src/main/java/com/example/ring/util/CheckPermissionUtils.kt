package com.example.ring.util

import android.Manifest
import android.content.Context
import java.nio.file.Files.size
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat





/**
 * Created by ZY on 2018/9/23.
 */
object CheckPermissionUtils {
    //需要申请的权限
    private val permissions = arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE)

    fun checkPermission(context: Context):Array<String>{
        val data = ArrayList<String>()//存储未申请的权限
        for (permission in permissions) {
            val checkSelfPermission = ContextCompat.checkSelfPermission(context, permission)
            if (checkSelfPermission == PackageManager.PERMISSION_DENIED) {//未申请
                data.add(permission)
            }
        }
        return data.toTypedArray()
    }
}