package com.example.ring.util

/**
 * Created by ZY on 2018/6/20.
 */
object StringUtil {

    /**
     * 十六进制String转换成Byte[]
     * @param hexString the hex string
     * *
     * @return byte[]
     */
    fun hexStringToBytes(hexString: String?): ByteArray? {
        var hexString = hexString
        if (hexString == null || hexString == "") {
            return null
        }
        hexString = hexString.toUpperCase()
        val length = hexString.length / 2
        val hexChars = hexString.toCharArray()
        val d = ByteArray(length)
        for (i in 0..length - 1) {
            val pos = i * 2
            d[i] = (charToByte(hexChars[pos]).toInt() shl 4 or charToByte(hexChars[pos + 1]).toInt()).toByte()
        }
        return d
    }

    fun charToByte(c: Char): Byte {
        return "0123456789ABCDEF".indexOf(c).toByte()
    }

    /* 这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
        * @param src byte[] data
        * @return hex string
        */
    fun bytesToHexString(src: ByteArray?): String? {
        val stringBuilder = StringBuilder("")
        if (src == null || src.size <= 0) {
            return null
        }
        for (i in 0..src.size-1) {
            val v = src[i].toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
        }
        return stringBuilder.toString()
    }

    //把Int数取低2字节 如：320 为40 01
    fun int2ByteArr2(a:Int):ByteArray{
        val byteArray = ByteArray(2)
        byteArray[0] = (a and 0xFF).toByte()
        byteArray[1] = (a shr 8 and 0xFF).toByte()
        return byteArray
    }

    fun byteArr22Int(byteArray: ByteArray):Int{
        val low = byteArray[0].toInt()
        val hig = byteArray[1].toInt()
        val i1 = (hig shl 8 and 0xFF00) or (low and 0xFF)
        return i1
    }

}