package tgm.mbaumgartner.station

import org.springframework.http.HttpStatus.*
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/stations")
class StationController(private var stationService: StationService) {
    @GetMapping("/")
    fun getStations(): ResponseEntity<Collection<Station>> = ResponseEntity.status(OK).body(stationService.stations.values)
    
    @GetMapping("/{stationId}")
    fun getStation(@PathVariable stationId: Int): ResponseEntity<Station> = ResponseEntity.status(OK).body(stationService.stations.getOrElse(stationId, null))

    @GetMapping("/{stationId}/measurements/latest")
    fun getStationMeasurements(@PathVariable stationId: Int): ResponseEntity<StationData> {
        
    }
}