package com.alsaeeddev.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import com.alsaeeddev.security.PrivacyLogger

@RequiresApi(Build.VERSION_CODES.N)
class PrivateCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        PrivacyLogger.i("CallScreeningService intercepted call for screening.")

        // In a privacy app, we allow the call to ring normally while ensuring metadata privacy
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }
}
