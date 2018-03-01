package com.apps.bilaleluneis.iottempraturemonitor

import android.bluetooth.BluetoothAdapter
import android.util.Log
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBluetoothAdapter


@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)

class BluetoothAdapterTest {

    private lateinit var blueToothAdapter: ShadowBluetoothAdapter

    @Before
    fun setup(){
        Log.d("Test", "setting up test")
        blueToothAdapter = Shadows.shadowOf(BluetoothAdapter.getDefaultAdapter())
    }

    @After
    fun tearDown(){
        Log.d("Test", "tearing down test!")
    }

    @Test
    fun initBluetoothAdapter() {

        blueToothAdapter?.let{
            Log.d("TestLogger", "BlueTooth Adapter is Available on SBC!")
            if(!it.isEnabled){ it.enable() }
           // it.name = "Test Adapter"
            //Log.d("TestLogger", "BlueTooth Name is ${it.name}")
        }

        assert(blueToothAdapter.isEnabled)
        //assert(blueToothAdapter.name.equals("Test Adapter", true))

    }
}
