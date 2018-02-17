package com.apps.bilaleluneis.iottempraturemonitor

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import kotlin.properties.Delegates



/**
 * @author Bilal El Uneis
 * @since Feb 2018
 * bilaleluneis@gmail.com
 * used Youtube Video about Android things as reference
 * https://www.youtube.com/watch?v=v3Dm5aeuQKE&t=1152s
 */

class TempratureMonitorActivity : Activity() {

    private val logTag = "TempratureMonitorActivity"
    private val tempratureSensor by lazy { initTempratureSensor() }
    private val display by lazy { initDisplay() }
    private val threadHandler = Handler(Looper.getMainLooper())
    private val blueToothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    /**
     This is cool! an observable property , similar to Swift willSet and didSet
     the property initial value is 0, I used _ for first two arguments in Lambda
     as i don't care for property and oldValue parameters and only need the newValue
     when I call this property with ++ and it reaches value of 2 I set it back to 0
     and clear the display.
     check out the counterToClearDisplay++ in [backgroundLooper]
     */
    private var counterToClearDisplay : Int by Delegates.observable(0){
        _, _, newValue ->
        if(newValue == 2) {
            counterToClearDisplay = 0
            display.clear()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        Log.d(logTag, "Inside OnCreate!")
        initBlueToothOnSBC()
        backgroundLooper()

    }

    override fun onDestroy() {

        super.onDestroy()
        Log.d(logTag, "Inside OnDestroy!")
        cleanUpBeforeExit()

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
        counterToClearDisplay ++
        //repeat every 10 seconds!
        threadHandler.postDelayed(this::backgroundLooper, 10000)

    }

    /**
     * init the bluetooth on the Rasberry PI or Single Board Computer
     */
    private fun initBlueToothOnSBC() {

        blueToothAdapter?.let{
            Log.d(logTag, "BlueTooth Adapter is Set!")
            it.enable()
            Log.d(logTag, "BlueTooth Enabled Flag is ${it.isEnabled}")
            return
        }

        println("BlueTooth Adapter was not Set !!")
    }

    /**
     * this method will be called onRestart() and onStop
     */
    private fun cleanUpBeforeExit() {

        Log.d(logTag, "Init Clean Before Existing or Restarting Activity!")
        threadHandler.removeCallbacksAndMessages(null)
        /**
         * this is another way instead of having to do
         * display.clear() display.setEnabled() display.close()
         */
        with(display){
            clear()
            setEnabled(false)
            close()
        }

        tempratureSensor.close()
        blueToothAdapter?.disable()
        Log.d(logTag, "Clean up completed !")

    }

    //TODO: need to debug this.. it is returning higher value than what should be correct!
    //or is it the hardware Sensor or driver that is reading higher number than it is?!!
    private fun toFahrenheit(celsius: Float) : Float = ((celsius * 1.8) + 32).toFloat()

}
