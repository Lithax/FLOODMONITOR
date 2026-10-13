package tgm.mbaumgartner

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tgm.mbaumgartner.station.Location
import tgm.mbaumgartner.station.Station
import tgm.mbaumgartner.station.StationService

@Configuration
class Configuration {
    @Bean
    fun stationService(): StationService {
        var stationService = StationService()
        
        var station1 = Station(
            0,
            "A2-Wien",
            "Donau",
            true,
            Location(48.2082, 16.3738),
            230.0, 300.0, 400.0
        )
        
        var station2 = Station(
            1,
            "A2-Salzburg",
            "Salzach",
            true,
            Location(47.8095, 13.0550),
            230.0, 300.0, 400.0
        )

        var station3 = Station(
            2,
            "A2-Graz",
            "Mur",
            true,
            Location(47.0707, 15.4395),
            230.0, 300.0, 400.0
        )
        
        putStation(stationService.stations, station1)
        putStation(stationService.stations, station2)
        putStation(stationService.stations, station3)
        
        return stationService
    }
    
    private fun putStation(map: MutableMap<Int, Station>, station: Station) {
        map[station.id] = station
    }
}