package tgm.mbaumgartner.station

import com.fasterxml.jackson.annotation.JsonUnwrapped

data class Station(
    var id: Int,
    var name: String,
    var river: String,
    var isActive: Boolean,
    @field:JsonUnwrapped
    var location: Location,
    var normalWaterLevel: Double,
    var warningWaterLevel: Double,
    var criticalWaterLevel: Double,
    var status: StationStatus = StationStatus.ONLINE
) {
    val measurements: MutableList<StationData> = ArrayList()
}