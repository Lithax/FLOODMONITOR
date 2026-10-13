package tgm.mbaumgartner.station

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/stations")
class StationController(private var stationService: StationService) {
    @GetMapping(produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStations(filter: StationFilter): ResponseEntity<StationsResponse> = ResponseEntity.status(OK).body(StationsResponse(stationService.filterStations(filter)))
    
    @GetMapping("/{stationId}", produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStation(@PathVariable stationId: Int): ResponseEntity<Station> = ResponseEntity.status(OK).body(stationService.stations.getValue(stationId))

    @GetMapping("/{stationId}/measurements", produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStationMeasurements(@PathVariable stationId: Int, filter: StationDataFilter): ResponseEntity<Collection<StationData>> = ResponseEntity.ok().body(stationService.filterMeasurements(stationId, filter))
    
    @GetMapping("/{stationId}/measurements/latest", produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStationLatestMeasurement(@PathVariable stationId: Int): ResponseEntity<StationData> = ResponseEntity.ok().body(stationService.getStation(stationId).measurements.last())
}


@JacksonXmlRootElement(localName = "stations")
data class StationsResponse(
    @get:JacksonXmlElementWrapper(useWrapping = false)
    @get:JacksonXmlProperty(localName = "station")
    val stations: Collection<Station>
)