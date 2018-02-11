package com.apps.bilaleluneis.iottempraturemonitor

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat

/**
 * @author Bilal El Uneis
 * @since Feb 2018
 * bilaleluneis@gmail.com
 * used Youtube Video about Android things as reference
 * https://www.youtube.com/watch?v=v3Dm5aeuQKE&t=1152s
 */

class TempratureMonitorActivity : Activity() {

    /* wanted to use TempratureMonitorActivity::javaClass.toString()
       but Kotlin reflection is not available I guess when doing embedded
       programming */
    private val logTag = "TempratureMonitorActivity"

    private val tempratureSensor by lazy { initTempratureSensor() }
    private val display by lazy { initDisplay() }
    private val looper = Handler(Looper.getMainLooper()) //TODO: read about this, and find what is better

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        Log.d(logTag, "Inside OnCreate!")
        backgroundLooper()

    }

    /**
     * trying to turn off sensors and display when activity is destroyed.
     * need to look into activity LC to understand what else i might need to
     * override for clean up and shutdown
     */
    //TODO: is the sequence correct? i destroy this props then super?
    //TODO: how do I turn off the display when i kill process in IDE?!!
    override fun onDestroy() {

        Log.d(logTag, "Inside OnDestroy!")
        display.close()
        tempratureSensor.close()
        super.onDestroy()

    }

    /**
     * again trying to reset and turnoff sensors
     */
    override fun onRestart() {

        super.onRestart()
        Log.d(logTag, "Inside OnRestart!!")

    }

    private fun initTempratureSensor() : Bmx280 {

        Log.d(logTag, "Init Temprature Sensor!")
        val sensor = RainbowHat.openSensor()
        sensor.setMode(Bmx280.MODE_NORMAL)
        sensor.temperatureOversampling = Bmx280.OVERSAMPLING_1X
        return sensor

    }

    private fun initDisplay() : AlphanumericDisplay {

        Log.d(logTag, "Init Display!")
        val display = RainbowHat.openDisplay()
        display.setEnabled(true)
        return display
    }

    /**
     * this method will basically be used to run operations on sensors
     * in the background and not on main thread .. so not to block the
     * main thread... need to read more about how this works!
     */
    private fun backgroundLooper() {

        Log.d(logTag, "executing backgroundLooper!")
        val temprature = toFahrenheit(tempratureSensor.readTemperature())
        display.display(temprature.toString())
        looper.postDelayed(this::backgroundLooper, 500)

    }

    private fun toFahrenheit(celsius: Float) : Float = ((celsius * 1.8) + 32).toFloat()

}
