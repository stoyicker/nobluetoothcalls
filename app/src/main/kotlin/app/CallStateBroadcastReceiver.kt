package app

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.telephony.TelephonyManager

internal class CallStateBroadcastReceiver : BroadcastReceiver() {
    private lateinit var sharedPreferences: SharedPreferences
    private var lastState = TelephonyManager.CALL_STATE_IDLE

    override fun onReceive(context: Context, intent: Intent) {
        sharedPreferences = context.getSharedPreferences(
                "CallStateBroadcastReceiver", Context.MODE_PRIVATE)
        lastState = sharedPreferences.getInt("lastState", TelephonyManager.CALL_STATE_IDLE)

        onCallStateChanged(when (intent.extras.getString(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            else -> throw IllegalStateException("Unrecognized TelephonyManager EXTRA state")
        })
    }

    private fun onCalling() {
        sharedPreferences.edit()
                .putBoolean("wasBluetoothEnabled", BluetoothAdapter.getDefaultAdapter().isEnabled)
                .apply()
        setBluetooth(false)
    }

    private fun onCallEnded() {
        setBluetooth(sharedPreferences.getBoolean("wasBluetoothEnabled", false))
    }

    private fun setBluetooth(enable: Boolean): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val isEnabled = bluetoothAdapter.isEnabled
        if (enable && !isEnabled) {
            return bluetoothAdapter.enable()
        } else if (!enable && isEnabled) {
            return bluetoothAdapter.disable()
        }
        // No need to change bluetooth state
        return true
    }

    private fun onCallStateChanged(state: Int) {
        if (lastState == state) {
            //No change, debounce extras
            return
        }
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                onCalling()
            }
            TelephonyManager.CALL_STATE_OFFHOOK ->
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    onCalling()
                }
                // Else we're picking up a ringing call
            TelephonyManager.CALL_STATE_IDLE ->
                // Went to idle - this is the end of a call.
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    onCallEnded()
                }
        }
        lastState = state
    }
}
