package com.apps.bilaleluneis.iottempraturemonitor

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*
import kotlin.properties.Delegates



/**
 * @author Bilal El Uneis
 * @since Feb 2018
 * bilaleluneis@gmail.com
 * used Youtube Video about Android things as reference
 * https://www.youtube.com/watch?v=v3Dm5aeuQKE&t=1152s
 */

//TODO: Thinking at some point take out the bluetooth stuff to diff file, class, etc
class TempratureMonitorActivity : Activity() {

    private val logTag = "TempratureMonitorActivity"
    private val tempratureSensor by lazy { initTempratureSensor() }
    private val display by lazy { initDisplay() }
    private val threadHandler = Handler(Looper.getMainLooper())
    private val blueToothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private val friendlyBlueToothName = "Temperature Monitor IoT"
    private val uuid = UUID.fromString("4e5d48e0-75df-11e3-981f-0800200c9a66")

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
            //launch{} will run the code inside in a lite async thread!
            launch{ listenToClientConnection() } //TODO: document this in Learning Kotlin!
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
        threadHandler.postDelayed({ this.sendTemperatureUpdatesToClientBluetooth(temprature.toString()) },10000)

    }

    /**
     *  send temperature updates to client
     */
    private fun sendTemperatureUpdatesToClientBluetooth(temperature : String) {

        Log.d(logTag, "inside sendTempUpdateToClient !")

        bluetoothSocket?.apply{
            Log.d(logTag, "checking if there is a client connected !")
            val messageToClientBluetooth = OutputStreamWriter(outputStream)
            if(isConnected){
                Log.d(logTag, "sending temperature value of $temperature to client ${remoteDevice.name} !")
                try{
                    messageToClientBluetooth.write(temperature)
                    messageToClientBluetooth.flush()
                    Log.d(logTag, "message sent and flushed !")
                }catch(exception: IOException){
                    Log.e(logTag, "IOException most likely during flush()")
                    messageToClientBluetooth.close()
                }
            }
        }

    }

    /**
     * this code will is blocking , so will need to be used with coroutines to run
     * and not end up blocking the main thread or UI thread.
     */
    private suspend fun listenToClientConnection() {

        Log.d(logTag, "Init bluetooth Server Socket !")
        blueToothAdapter?.listenUsingRfcommWithServiceRecord(friendlyBlueToothName, uuid)?.apply {
            while(true){
                delay(2)
                if(bluetoothSocket == null || !bluetoothSocket?.isConnected!!){
                    Log.d(logTag, "Server Socket is listening for client connection....")
                    bluetoothSocket = accept()
                    Log.d(logTag,"${bluetoothSocket?.remoteDevice?.name} is connected to IoT bluetooth")
                    close() // this will close connection ensuring only one device is connected !
                }
            }
        }

    }

    /**
     * init the bluetooth on the Raspberry PI 3B or Single Board Computer
     * this should work on any SBC that has bluetooth component.
     * the method will check that adapter is not null, then will enable it
     * if it is not enabled and finally assign a name "Temperature Monitor IoT"
     * to the [blueToothAdapter]
     */
    private fun initBlueToothOnSBC() : Boolean{

        blueToothAdapter?.apply{
            Log.d(logTag, "BlueTooth Adapter is Available on SBC!")
            if(!isEnabled){
                Log.d(logTag, "Bluetooth adapter is disabled ... Enabling !")
                enable()
                Log.d(logTag, "Bluetooth adapter is now Enabled !")
            }
            name = friendlyBlueToothName
            Log.d(logTag, "BlueTooth Name is $name")
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
    //TODO: change this to run in background every 3 mins and discoverable for 1  mins at a time
    private fun enableBlueToothDiscoveryMode() {

        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            // using value 0 which keeps this bluetooth discoverable always.. will drain battery!
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0)
            //requestCode is just value sent.. don't really care for it for this application
            startActivityForResult(this,100) //TODO:create const
        }

    }

    /**
     * perform cleanups on resources and sensors
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
        bluetoothSocket?.close()
        blueToothAdapter?.disable()
        Log.d(logTag, "Clean up completed !")

    }

    //TODO: need to debug this.. it is returning higher value than what should be correct!
    //or is it the hardware Sensor or driver that is reading higher number than it is?!!
    private fun toFahrenheit(celsius: Float) : Float = ((celsius * 1.8) + 32).toFloat()

}
