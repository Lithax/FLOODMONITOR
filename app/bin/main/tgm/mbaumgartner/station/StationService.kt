package tgm.mbaumgartner.station

import org.springframework.stereotype.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import kotlin.random.Random
import kotlin.time.Clock
import java.time.Instant

@Service
class StationService {
    companion object {
        const val MAX_LIMIT = 1000
    }

    @field:Value($$"${simulation.enabled}")
    var isEnabled: Boolean = true
    val stations: MutableMap<Int, Station> = HashMap()

    fun getStation(id: Int): Station {
        return stations[id] ?: throw NoSuchElementException("Station with id $id not found")
    }
    
    fun filterMeasurements(id: Int, filter: StationDataFilter): List<StationData> {
        val measurements = getStation(id).measurements
        val from = filter.from
        val to = filter.to
        val limit = filter.limit
        require(limit == null || limit in 1..MAX_LIMIT) { "limit must be between 1 and $MAX_LIMIT" }
        require(from == null || to == null || from <= to) { "'from' must not be after 'to'" }
        val result = measurements.filter(filter::matches)
        return if (limit == null) result else result.takeLast(limit)
    }

    // NORMAL below the warning value, WARNING from the warning value, CRITICAL from the critical value
    fun calculateWarningLevel(station: Station, waterLevel: Double): WarningLevel = when {
        waterLevel >= station.criticalWaterLevel -> WarningLevel.CRITICAL
        waterLevel >= station.warningWaterLevel -> WarningLevel.WARNING
        else -> WarningLevel.NORMAL
    }

    fun filterStations(filter: StationFilter): List<Station> {
        return stations.values.filter(filter::matches)
    }
    
    fun filterStations(filter: StationAlertFilter): List<Station> {
        return stations.values.filter(filter::matches)
    }

    @Scheduled(fixedRateString = $$"${simulation.interval}")
    fun generateMeasurements() {
        if (!isEnabled) return
        
        for ((id, station) in stations) {
            if(!station.isActive || station.status == StationStatus.OFFLINE) continue
            
            var now: Instant = Instant.now()
            var newWaterLevel: Double
            var newFlowRate: Double
            var newRainfall: Double
            var newTemperature: Double
            var newBatteryLevel: Double
            var newStationStatus: StationStatus = station.status
            
            if(station.measurements.isEmpty()) {
                newWaterLevel = station.normalWaterLevel + (Math.random()*10)-5
                newFlowRate = newWaterLevel * 2.0
                newRainfall = 0.0
                newBatteryLevel = if (100.0 - Math.random() > 0.7) (Math.random() * 7) else 0.0
                newTemperature = 15.0 + (Math.random()*3)-1.5
            } else {
                var prev = station.measurements.last()
                
                newFlowRate = (prev.waterLevel * 2.5).coerceAtLeast(0.0)
                newTemperature = (prev.temperature + kotlin.random.Random.nextDouble(-0.2, 0.2)).coerceIn(-5.0, 35.0)
                
                val rainImpact = prev.rainfall * 0.4
                val naturalDrain = (prev.waterLevel - station.normalWaterLevel) * 0.05
                val noise = Random.nextDouble(-1.0, 1.0)

                newWaterLevel = (prev.waterLevel + rainImpact - naturalDrain + noise).coerceAtLeast(0.0)
                
                val deltaRain = Random.nextDouble(-2.0, 2.5)
                newRainfall = (prev.rainfall + deltaRain).coerceAtLeast(0.0)
                
                newBatteryLevel = (prev.batteryLevel - Random.nextDouble(0.01, 0.05)).coerceIn(0.0, 100.0)
            }
            
            if(newBatteryLevel <= 0.0) newStationStatus = StationStatus.OFFLINE
            if(newBatteryLevel <= 15.0) newStationStatus = StationStatus.MAINTENANCE
            
            val newWarningLevel = calculateWarningLevel(station, newWaterLevel)
            
            station.measurements.add(
                StationData(
                    now, 
                    newWaterLevel,
                    newFlowRate,
                    newRainfall,
                    newTemperature,
                    newBatteryLevel,
                    newStationStatus,
                    newWarningLevel,
                )
            )
        }
    }
}

data class StationFilter(
    val river: String? = null,
    val isActive: Boolean? = null,
    val warningLevel: WarningLevel? = null
) {
    fun matches(station: Station): Boolean {
        if (river != null && !station.river.equals(river, ignoreCase = true)) return false
        if (isActive != null && station.isActive != isActive) return false
        if (warningLevel != null && (station.measurements.isEmpty() || station.measurements.last().warningLevel != warningLevel)) return false
        return true
    }
}

data class StationDataFilter(
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int? = null,
    val warningLevel: WarningLevel? = null
) {
    fun matches(stationData: StationData): Boolean {
        if(from != null && stationData.timestamp < from) return false
        if(to != null && stationData.timestamp > to) return false
        if (warningLevel != null && warningLevel != stationData.warningLevel) return false
        return true
    }
}

data class StationAlertFilter(
    val warningLevel: WarningLevel? = null,
    val river: String? = null
) {
    fun matches(station: Station): Boolean {
        if (river != null && !station.river.equals(river, ignoreCase = true)) return false
        if (warningLevel != null && (station.measurements.isEmpty() || station.measurements.last().warningLevel != warningLevel)) return false
        return true
    }
}