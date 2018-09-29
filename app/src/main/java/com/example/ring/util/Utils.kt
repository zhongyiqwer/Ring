package com.example.ring.util

import android.content.Context
import android.net.ConnectivityManager

/**
 * Created by ZY on 2018/9/28.
 */
object Utils {
    fun isNetworkAvailable(context: Context):Boolean{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return !(networkInfo==null || !networkInfo.isAvailable)
    }
}