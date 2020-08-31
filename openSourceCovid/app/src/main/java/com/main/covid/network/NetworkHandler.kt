package com.main.covid.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import org.jetbrains.anko.runOnUiThread


/**
 * Created by Rubén Izquierdo, Global Incubator
 */
class NetworkHandler
constructor(private val context: Context) {
    val isConnected : Boolean
        get() {
            val result: Boolean
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                    connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }


            return result
        }
}

interface ActivityNetwork
{
    fun isConnected()
    fun isDisconnected()
}

interface INetworkContext
{

    fun initNetworkScan(context: Application, activityNetwork: ActivityNetwork)
    fun cancelNetworkScan(context: Application)

    class NetworkContext : INetworkContext
    {

        private lateinit var netWorkCallback: ConnectivityManager.NetworkCallback

        override fun initNetworkScan(context: Application, activityNetwork: ActivityNetwork) {


            netWorkCallback = object: ConnectivityManager.NetworkCallback() {

                override fun onLost(network: Network?) {
                    //record wi-fi disconnect event
                    context.runOnUiThread{
                        activityNetwork.isDisconnected()

                    }
                }override fun onUnavailable() {



                }override fun onLosing(network: Network?, maxMsToLive: Int) {

                }override fun onAvailable(network: Network?) {
                    //record wi-fi connect event
                    context.runOnUiThread {
                        activityNetwork.isConnected()

                    }

                }
            }


            val connectivityManager =
                    context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .build()

            connectivityManager.registerNetworkCallback(networkRequest, netWorkCallback)

            val networkCapabilities = connectivityManager.activeNetwork
            val actNw =
                    connectivityManager.getNetworkCapabilities(networkCapabilities)
            val result = when {
                actNw?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false -> true
                actNw?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false -> true
                actNw?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ?: false -> true
                else -> false
            }

            if(result)
                context.runOnUiThread { activityNetwork.isConnected() }
            else
                context.runOnUiThread{ activityNetwork.isDisconnected() }


        }

        override fun cancelNetworkScan(context: Application) {

            val connectivityManager =
                    context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if(::netWorkCallback.isInitialized)
                connectivityManager.unregisterNetworkCallback(netWorkCallback)

        }


    }

}

