package com.example.ring.util

import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by ZY on 2018/10/1.
 */
object LogUtil {
    private const val SWICH = true
    private var PATH = ""
    private var dataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    fun d(tag: String, msg: Any) {// 调试信息
        log(tag, msg.toString())
    }

    private fun log(tag: String, toString: String) {
        if (SWICH){
            val data = Date()
            val format = dataFormat.format(data)
            val content = "$format tag:$tag msg:$toString"
            val file = getLogPath()
            val fileWriter = FileWriter(file, true)
            val bufferedWriter = BufferedWriter(fileWriter)
            bufferedWriter.write(content)
            bufferedWriter.newLine()
            bufferedWriter.close()
            fileWriter.close()
        }
    }

    private fun getLogPath() :File{
        if (android.os.Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED){
            PATH = Environment.getExternalStorageDirectory().absolutePath+File.separator+"ringLog"
        }
        val file = File(PATH)
        if (!file.exists()){
            file.mkdirs()
        }
        val file1 = File(PATH + File.separator + "log.txt")
        return file1
    }
}