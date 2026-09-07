package com.alsaeeddev.presentation.simulation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alsaeeddev.telecom.CallManager

data class SimulationPreset(
    val title: String,
    val description: String,
    val callerName: String,
    val callerNumber: String,
    val location: String
)

class SimulationViewModel(application: Application) : AndroidViewModel(application) {

    private val callManager = CallManager.getInstance(application)

    val presets = listOf(
        SimulationPreset(
            title = "Personal Contact",
            description = "Simulate an incoming call from a close friend or family member",
            callerName = "Alex Morgan",
            callerNumber = "+1 (555) 234-5678",
            location = "Seattle, WA"
        ),
        SimulationPreset(
            title = "Sensitive Medical / Legal",
            description = "High-privacy medical clinic caller identity protection test",
            callerName = "Saint Jude Medical Center",
            callerNumber = "+1 (800) 555-8910",
            location = "Memphis, TN"
        ),
        SimulationPreset(
            title = "Financial Institution",
            description = "Confidential banking advisory call simulation",
            callerName = "First National Private Banking",
            callerNumber = "+1 (888) 555-4321",
            location = "New York, NY"
        ),
        SimulationPreset(
            title = "Unknown / Hidden Caller",
            description = "Incoming call with restricted identity markers",
            callerName = "Private Number",
            callerNumber = "+1 (555) 000-0000",
            location = "Restricted"
        )
    )

    fun startCustomSimulation(name: String, number: String, location: String) {
        callManager.startSimulatedCall(
            callerName = name.ifBlank { "Unknown Caller" },
            callerNumber = number.ifBlank { "+1 (555) 123-4567" },
            location = location.ifBlank { "Cellular Call" }
        )
    }

    fun startPresetSimulation(preset: SimulationPreset) {
        callManager.startSimulatedCall(
            callerName = preset.callerName,
            callerNumber = preset.callerNumber,
            location = preset.location
        )
    }
}
