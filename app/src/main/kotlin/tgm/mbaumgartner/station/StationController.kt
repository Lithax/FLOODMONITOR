package tgm.mbaumgartner.station

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/v1/stations")
@Tag(name = "Stations", description = "Measuring stations and their measurements")
class StationController(private var stationService: StationService) {
    @Operation(summary = "Get all stations", description = "Optionally filtered by river, isActive and the current warningLevel.")
    @GetMapping(produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStations(@ParameterObject filter: StationFilter): ResponseEntity<StationsResponse> = ResponseEntity.status(OK).body(StationsResponse(stationService.filterStations(filter)))
    
    @Operation(summary = "Get a single station", description = "404 if the station id is unknown.")
    @GetMapping("/{stationId}", produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStation(@PathVariable stationId: Int): ResponseEntity<Station> = ResponseEntity.status(OK).body(stationService.stations.getValue(stationId))

    @Operation(summary = "Get historical measurements", description = "Filter by from/to (ISO-8601), warningLevel and limit (1-1000, returns the most recent ones). 400 if limit is out of range or from is after to.")
    @GetMapping("/{stationId}/measurements", produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStationMeasurements(@PathVariable stationId: Int, @ParameterObject filter: StationDataFilter): ResponseEntity<Collection<StationData>> = ResponseEntity.ok().body(stationService.filterMeasurements(stationId, filter))
    
    @Operation(summary = "Get the latest measurement of a station", description = "404 if the station is unknown or has no measurement yet.")
    @GetMapping("/{stationId}/measurements/latest", produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE])
    fun getStationLatestMeasurement(@PathVariable stationId: Int): ResponseEntity<StationData> = ResponseEntity.ok().body(stationService.getStation(stationId).measurements.last())
}


@JacksonXmlRootElement(localName = "stations")
data class StationsResponse(
    @get:JacksonXmlElementWrapper(useWrapping = false)
    @get:JacksonXmlProperty(localName = "station")
    val stations: Collection<Station>
)