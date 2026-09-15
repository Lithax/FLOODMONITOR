package tgm.mbaumgartner.station

import org.springframework.stereotype.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled

@Service
class StationService(
    @field:Value($$"${simulation.interval}")
    var interval: Long,
    @field:Value($$"${simulation.enabled}")
    var isEnabled: Boolean
) {
    val stations: Map<Int, Station> = HashMap()

    @Scheduled(fixedRateString = $$"${simulation.interval}")
    fun generateMeasurements() {
        if (!isEnabled) return
        
        for ((id, station) in stations) {
            
        }
    }
}