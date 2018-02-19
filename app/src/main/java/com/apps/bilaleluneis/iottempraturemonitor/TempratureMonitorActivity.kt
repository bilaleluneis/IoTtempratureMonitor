package com.apps.bilaleluneis.iottempraturemonitor

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
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
    private val friendlyBlueToothName = "Temperature Monitor"

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

    /**
     * after some reading, I think this is the best place to put initialization code
     * instead of [onCreate].. because onStart() will get called always after onCreate()
     * or [onRestart]. and if I do debugging using instant run (hot code swap) then
     * the onCreate() wont get called .. instead onRestart() will. so this way I can
     * account for both when activity is created or restarted.
     */
    override fun onStart() {

        super.onStart()
        Log.d(logTag, "Inside OnStart!")
        if( initBlueToothOnSBC() ){
            Log.d(logTag, "Enabling BlueTooth Discovery Mode !")
            enableBlueToothDiscoveryMode()
        }
        backgroundLooper()

    }

    /**
     * after some reading. onStop is best place to do clean up instead of [onDestroy],
     * onStop() will get called before onDestroy() and [onRestart].
     * and instant run (hot code swap) will call onStop() then on Restart(),
     * while exiting or killing the process will call onStop() then on Destroy()
     */
    override fun onStop() {

        super.onStop()
        Log.d(logTag, "Inside OnStop!")
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
     * init the bluetooth on the Raspberry PI 3B or Single Board Computer
     * this should work on any SBC that has bluetooth component
     */
    private fun initBlueToothOnSBC() : Boolean{

        blueToothAdapter?.let{
            Log.d(logTag, "BlueTooth Adapter is Available on SBC!")
            if(!it.isEnabled){ it.enable() }
            it.name = friendlyBlueToothName
            Log.d(logTag, "BlueTooth Name is ${it.name}")
            return true
        }

        println("BlueTooth Adapter not Available on SBC.. No BlueTooth Support !")
        return false

    }

    /**
     * this basically will make the Bluetooth on SBC discoverable
     * by sending out broadcast with the Bluetooth name for listeners
     * to find.
     */
    private fun enableBlueToothDiscoveryMode() {

        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300) //TODO: create const
            startActivityForResult(this,100) //TODO:create const
        }

    }

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
