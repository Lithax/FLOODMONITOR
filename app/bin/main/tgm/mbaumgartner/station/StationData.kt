package tgm.mbaumgartner.station

import kotlin.time.Instant

data class StationData(
    var timestamp: Instant,
    var waterLevel: Double,
    var flowRate: Double,
    var rainfall: Double,
    var temperature: Double,
    var batteryLevel: Double,
    var status: StationStatus,
    var warningLevel: WarningLevel
)