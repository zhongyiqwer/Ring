package com.example.ring.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings


/**
 * Created by ZY on 2018/9/25.
 */
object SettingUtils {

    fun enterWhiteListSetting(context: Context) {
        try {
            context.startActivity(getSettingIntent())
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun getSettingIntent(): Intent {

        var componentName: ComponentName? = null

        val brand = android.os.Build.BRAND

        when (brand.toLowerCase()) {
            "samsung" -> componentName = ComponentName("com.samsung.android.sm",
                    "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity")
            "huawei" -> componentName = ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            "xiaomi" -> componentName = ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity")
            "vivo" -> componentName = ComponentName("com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            "oppo" -> componentName = ComponentName("com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
            "360" -> componentName = ComponentName("com.yulong.android.coolsafe",
                    "com.yulong.android.coolsafe.ui.activity.autorun.AutoRunListActivity")
            "meizu" -> componentName = ComponentName("com.meizu.safe",
                    "com.meizu.safe.permission.SmartBGActivity")
            "oneplus" -> componentName = ComponentName("com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
            "nubia"->{
                println("nubia")
                componentName = ComponentName("cn.nubia.security2",
                        "cn.nubia.security.appmanage.selfstart.ui.SelfStartActivity")
            }
            else -> {
            }
        }

        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (componentName != null) {
            intent.component = componentName
        } else {
            intent.action = Settings.ACTION_SETTINGS
        }
        return intent
    }
}