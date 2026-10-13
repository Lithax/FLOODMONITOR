package tgm.mbaumgartner

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tgm.mbaumgartner.station.StationData
import tgm.mbaumgartner.station.StationService
import tgm.mbaumgartner.station.StationStatus
import tgm.mbaumgartner.station.WarningLevel
import java.time.Instant

// The simulator is switched off and all measurements are seeded by hand, so nothing here is random.
// Station 0 (A2-Wien): normal 230, warning 300, critical 400.
@SpringBootTest(properties = ["simulation.enabled=false"])
class StationApiTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var stationService: StationService

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build()
        stationService.stations.values.forEach { it.measurements.clear() }
    }

    private fun seed(time: String, waterLevel: Double) {
        val station = stationService.getStation(0)
        station.measurements.add(
            StationData(
                Instant.parse(time), waterLevel, waterLevel * 2.5, 0.0, 15.0, 90.0,
                StationStatus.ONLINE, stationService.calculateWarningLevel(station, waterLevel)
            )
        )
    }

    // 1
    @Test
    fun getAllStationsAsJson() {
        mvc.perform(get("/stations").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.stations.length()").value(3))
    }

    // 2
    @Test
    fun getAllStationsAsXml() {
        mvc.perform(get("/stations").accept(MediaType.APPLICATION_XML))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(xpath("/stations/station").nodeCount(3))
    }

    // 3
    @Test
    fun getExistingStation() {
        mvc.perform(get("/stations/0").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(0))
            .andExpect(jsonPath("$.name").value("A2-Wien"))
    }

    // 4
    @Test
    fun getUnknownStationReturns404() {
        mvc.perform(get("/stations/99").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    // 5
    @Test
    fun getLatestMeasurement() {
        seed("2026-05-01T08:00:00Z", 100.0)
        seed("2026-05-01T09:00:00Z", 320.0)

        mvc.perform(get("/stations/0/measurements/latest").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.waterLevel").value(320.0))
            .andExpect(jsonPath("$.warningLevel").value("WARNING"))
    }

    // 6
    @Test
    fun filterMeasurementsByTimeRange() {
        seed("2026-05-01T08:00:00Z", 100.0)
        seed("2026-05-01T10:00:00Z", 200.0)
        seed("2026-05-01T12:00:00Z", 310.0)

        mvc.perform(
            get("/stations/0/measurements")
                .param("from", "2026-05-01T09:00:00Z")
                .param("to", "2026-05-01T11:00:00Z")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].waterLevel").value(200.0))
    }

    // 7
    @Test
    fun fromAfterToReturns400() {
        mvc.perform(
            get("/stations/0/measurements")
                .param("from", "2026-05-01T12:00:00Z")
                .param("to", "2026-05-01T08:00:00Z")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    // 8
    @Test
    fun warningLevelNormalBelowWarningValue() {
        assertEquals(WarningLevel.NORMAL, stationService.calculateWarningLevel(stationService.getStation(0), 299.9))
    }

    // 9
    @Test
    fun warningLevelWarningFromWarningValue() {
        val station = stationService.getStation(0)
        assertEquals(WarningLevel.WARNING, stationService.calculateWarningLevel(station, 300.0))
        assertEquals(WarningLevel.WARNING, stationService.calculateWarningLevel(station, 399.9))
    }

    // 10
    @Test
    fun warningLevelCriticalFromCriticalValue() {
        assertEquals(WarningLevel.CRITICAL, stationService.calculateWarningLevel(stationService.getStation(0), 400.0))
    }

    // 11
    @Test
    fun unsupportedFormatReturns406() {
        mvc.perform(get("/stations").accept("text/csv"))
            .andExpect(status().isNotAcceptable)
    }

    // 12
    @Test
    fun limitAboveMaximumReturns400() {
        mvc.perform(get("/stations/0/measurements").param("limit", "1001").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun limitAtMaximumIsAccepted() {
        seed("2026-05-01T08:00:00Z", 100.0)

        mvc.perform(get("/stations/0/measurements").param("limit", "1000").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun negativeLimitReturns400() {
        mvc.perform(get("/stations/0/measurements").param("limit", "-1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun invalidDateFormatReturns400() {
        mvc.perform(get("/stations/0/measurements").param("from", "yesterday").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun invalidWarningLevelReturns400() {
        mvc.perform(get("/stations").param("warningLevel", "PURPLE").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
    }
}
