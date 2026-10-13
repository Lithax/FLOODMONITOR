package tgm.mbaumgartner.station

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Current warnings")
class AlertController(private var stationService: StationService) {
    @Operation(summary = "Get current warnings", description = "Current warning level per station, optionally filtered by warningLevel and river.")
    @GetMapping(produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStationMeasurements(@ParameterObject filter: StationAlertFilter): ResponseEntity<List<WarningLevel>> {
        var alerts: MutableList<WarningLevel> = ArrayList()
        for(station in stationService.filterStations(filter)) {
            var measurements = station.measurements
            if(measurements.isEmpty()) continue
            alerts.add(measurements.last().warningLevel)
        }
        return ResponseEntity.ok().body(alerts)
    }
}