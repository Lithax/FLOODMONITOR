package tgm.mbaumgartner.station

import org.springframework.http.MediaType.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class AlertController(private var stationService: StationService) {
    @GetMapping("/alerts", produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStationMeasurements(stationAlertFilter: StationAlertFilter): ResponseEntity<List<WarningLevel>> {
        var alerts: MutableList<WarningLevel> = ArrayList()
        for(station in stationService.filterStations(stationAlertFilter)) {
            var measurements = station.measurements
            if(measurements.isEmpty()) continue
            alerts.add(measurements.last().warningLevel)
        }
        return ResponseEntity.ok().body(alerts)
    }
}