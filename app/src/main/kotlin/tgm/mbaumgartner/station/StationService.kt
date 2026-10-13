package tgm.mbaumgartner.station

import org.springframework.stereotype.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

@Service
class StationService {
    @field:Value($$"${simulation.enabled}")
    var isEnabled: Boolean = true
    val stations: MutableMap<Int, Station> = HashMap()

    fun getStation(id: Int): Station {
        return stations[id] ?: throw NoSuchElementException("Station with id $id not found")
    }
    
    fun filterMeasurements(id: Int, filter: StationDataFilter): List<StationData> {
        var stations = getStation(id).measurements.filter(filter::matches)
        if(filter.limit != null)
            if(filter.limit < 1)
                return stations.subList(0, filter.limit)
            else throw IllegalArgumentException("Filter limit > 0")
        return stations
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
            
            var now: Instant = Clock.System.now()
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
            
            var newWarningLevel = when {
                newWaterLevel <= station.normalWaterLevel -> WarningLevel.NORMAL
                newWaterLevel < station.criticalWaterLevel -> WarningLevel.WARNING
                else -> WarningLevel.CRITICAL
            }
            
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
        if(from != null && from < stationData.timestamp) return false
        if(to != null && to > stationData.timestamp) return false
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