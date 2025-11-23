package com.lzylym.zymview.utils.wifi

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast

import androidx.annotation.RequiresApi

import java.util.*

object ZYMWifiConnector {

    private const val TAG = "WifiConnector"
    const val REQUEST_CODE_WIFI_ADD = 1010

    fun connectWifi(activity: Activity, ssid: String, password: String,isReal:Boolean) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                if (!isReal)
                    connectWithSuggestionIntentFake(activity, ssid, password)
                else
                    connectWithSuggestionIntentReal(activity,ssid,password)
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> connectTemporarily(activity, ssid, password)
            else -> connectLegacy(activity, ssid, password)
        }
    }

    // Android 11+
    @RequiresApi(Build.VERSION_CODES.R)
    private fun connectWithSuggestionIntentFake(activity: Activity, ssid: String, password: String) {
        val fakeSuggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(generateRandomPassword())
            .build()
        val list = ArrayList<WifiNetworkSuggestion>()
        list.add(fakeSuggestion)
        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
        intent.putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, list)
        activity.startActivityForResult(intent, REQUEST_CODE_WIFI_ADD)
    }

    // Android 11+
    @RequiresApi(Build.VERSION_CODES.R)
    private fun connectWithSuggestionIntentReal(activity: Activity, ssid: String, password: String) {
        val realSuggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()
        val list = ArrayList<WifiNetworkSuggestion>()
        list.add(realSuggestion)
        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
        intent.putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, list)
        activity.startActivity(intent)
    }

    private fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12)
            .map { chars.random() }
            .joinToString("")
    }

    // Android 10
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectTemporarily(activity: Activity, ssid: String, password: String) {
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val dialog = AlertDialog.Builder(activity)
            .setTitle("连接 Wi-Fi")
            .setMessage("正在连接 $ssid ...")
            .setCancelable(false)
            .create()
        dialog.show()

        cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "已临时连接到 Wi-Fi", Toast.LENGTH_SHORT).show()
                    dialog.setMessage("连接成功")
                    dialog.dismiss()
                }
                Log.d(TAG, "临时连接成功: $ssid")
            }

            override fun onUnavailable() {
                activity.runOnUiThread {
                    Toast.makeText(activity, "连接失败", Toast.LENGTH_SHORT).show()
                    dialog.setMessage("连接失败")
                    dialog.dismiss()
                }
                Log.e(TAG, "用户拒绝连接或者网络不可用")
            }
        })
    }

    // Android 9 及以下
    private fun connectLegacy(activity: Activity, ssid: String, password: String) {
        val wifiManager = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val config = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
            status = WifiConfiguration.Status.ENABLED
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("连接 Wi-Fi")
            .setMessage("正在连接 $ssid ...")
            .setCancelable(false)
            .create()
        dialog.show()

        val networkId = wifiManager.addNetwork(config)
        if (networkId != -1) {
            wifiManager.disconnect()
            wifiManager.enableNetwork(networkId, true)
            wifiManager.reconnect()

            activity.runOnUiThread {
                Toast.makeText(activity, "已保存并连接到 Wi-Fi", Toast.LENGTH_SHORT).show()
                dialog.setMessage("连接成功")
                dialog.dismiss()
            }
            Log.d(TAG, "连接成功: $ssid")
        } else {
            activity.runOnUiThread {
                Toast.makeText(activity, "添加网络失败", Toast.LENGTH_SHORT).show()
                dialog.setMessage("连接失败")
                dialog.dismiss()
            }
            Log.e(TAG, "添加网络失败")
        }
    }

    // 处理回调（可选，用于 Android 11+）
    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == REQUEST_CODE_WIFI_ADD) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "用户已保存网络")
            } else {
                Log.e(TAG, "用户取消保存 Wi-Fi")
            }
        }
    }
}
