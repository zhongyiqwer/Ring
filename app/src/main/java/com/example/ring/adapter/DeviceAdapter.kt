package com.example.massor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.clj.fastble.BleManager
import com.clj.fastble.data.BleDevice
import com.example.ring.R
import kotlinx.android.synthetic.main.adapter_device.view.*
import java.util.*

/**
 * Created by Administrator on 2018/6/1.
 */
class DeviceAdapter :BaseAdapter{

    private val context: Context
    private var bleDeviceList: ArrayList<BleDevice>

    constructor(context: Context) : super() {
        this.context = context
        bleDeviceList = ArrayList()
    }

    fun addDevice(bleDevice: BleDevice) {
        removeDevice(bleDevice)
        bleDeviceList.add(bleDevice)
    }

    fun removeDevice(bleDevice: BleDevice) {
        var y :Int?=0
        for (i in bleDeviceList.indices) {
            val device = bleDeviceList[i-y!!]
            if (bleDevice.key == device.key) {
                bleDeviceList.removeAt(i-y)
                y++
            }
        }
    }

    fun clearConnectedDevice() {
        var y :Int?=0
        for (i in bleDeviceList.indices) {
            val device = bleDeviceList[i-y!!]
            if (BleManager.getInstance().isConnected(device)) {
                bleDeviceList.removeAt(i-y)
                y++
            }
        }
    }

    fun clearScanDevice() {
        var y :Int?=0
        for (i in bleDeviceList.indices) {
            val device = bleDeviceList[i-y!!]
            if (!BleManager.getInstance().isConnected(device)) {
                bleDeviceList.removeAt(i-y)
                y++
            }
        }
    }

    fun clear() {
        clearConnectedDevice()
        clearScanDevice()
    }

    override fun getCount(): Int {
        return bleDeviceList.size
    }

    override fun getItem(position: Int): BleDevice? {
        return if (position > bleDeviceList.size) null else bleDeviceList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var holder: ViewHolder
        var view : View
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate( R.layout.adapter_device, parent,false)
            holder = ViewHolder()
            holder.img_blue = view.img_blue
            holder.txt_name = view.txt_name
            holder.txt_mac = view.txt_mac
            holder.txt_rssi = view.txt_rssi
            holder.layout_idle = view.layout_idle
            holder.layout_connected = view.layout_connected
            holder.btn_disconnect = view.btn_disconnect
            holder.btn_connect = view.btn_connect
            view.tag = holder

        }else{
            view = convertView
            holder = view.tag as ViewHolder
        }

        val bleDevice = getItem(position)
        if (bleDevice != null) {
            val isConnected = BleManager.getInstance().isConnected(bleDevice)
            val name = bleDevice.name
            val mac = bleDevice.mac
            val rssi = bleDevice.rssi
            holder.txt_name!!.text = name
            holder.txt_mac!!.text = mac
            holder.txt_rssi!!.text = rssi.toString()
            if (isConnected) {
                holder.img_blue!!.setImageResource(R.mipmap.ic_blue_connected)
                holder.txt_name!!.setTextColor(-0xe2164a)
                holder.txt_mac!!.setTextColor(-0xe2164a)
                holder.layout_idle!!.visibility = View.GONE
                holder.layout_connected!!.visibility = View.VISIBLE
            } else {
                holder.img_blue!!.setImageResource(R.mipmap.ic_blue_remote)
                holder.txt_name!!.setTextColor(-0x1000000)
                holder.txt_mac!!.setTextColor(-0x1000000)
                holder.layout_idle!!.visibility = View.VISIBLE
                holder.layout_connected!!.visibility = View.GONE
            }
        }

        holder.btn_connect!!.setOnClickListener {
            if (context.getSharedPreferences("Ble",0).getString("phone","").isNotEmpty()){
                if (mListener != null) {
                    mListener!!.onConnect(bleDevice)
                }
            }else{
                if (mListener != null) {
                    mListener!!.onSelectPhone()
                }
            }
        }

        holder.btn_disconnect!!.setOnClickListener {
            if (mListener != null) {
                mListener!!.onDisConnect(bleDevice)
            }
        }
        return view
    }

    internal inner class ViewHolder {
        var img_blue: ImageView? = null
        var txt_name: TextView? = null
        var txt_mac: TextView? = null
        var txt_rssi: TextView? = null
        var layout_idle: LinearLayout? = null
        var layout_connected: LinearLayout? = null
        var btn_disconnect: Button? = null
        var btn_connect: Button? = null
    }

    interface OnDeviceClickListener {
        fun onConnect(bleDevice: BleDevice?)

        fun onDisConnect(bleDevice: BleDevice?)

        fun onSelectPhone()
    }

    private var mListener: OnDeviceClickListener? = null

    fun setOnDeviceClickListener(listener: OnDeviceClickListener) {
        this.mListener = listener
    }
}