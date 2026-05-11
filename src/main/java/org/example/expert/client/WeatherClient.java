package org.example.expert.client;

import org.example.expert.client.dto.WeatherDto;
import org.example.expert.domain.common.exception.ServerException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class WeatherClient {

    private final RestTemplate restTemplate;

    public WeatherClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public String getTodayWeather() {
        ResponseEntity<WeatherDto[]> responseEntity =
                restTemplate.getForEntity(buildWeatherApiUri(), WeatherDto[].class);

        try {
            responseEntity = restTemplate.getForEntity(buildWeatherApiUri(), WeatherDto[].class);
        } catch (Exception e) {
            // RestTemplate이 던지는 4xx, 5xx 예외를 여기서 가로채서 우리 에러로 변환!
            throw new ServerException("날씨 데이터를 가져오는데 실패했습니다. 원인: " + e.getMessage());
        }

        WeatherDto[] weatherArray = responseEntity.getBody();

        // 2. 불필요한 else 블록 제거 후, 정상 응답일 경우의 검증 로직 수행
        if (weatherArray == null || weatherArray.length == 0) {
            throw new ServerException("날씨 데이터가 없습니다.");
        }

        String today = getCurrentDate();

        for (WeatherDto weatherDto : weatherArray) {
            if (today.equals(weatherDto.getDate())) {
                return weatherDto.getWeather();
            }
        }

        throw new ServerException("오늘에 해당하는 날씨 데이터를 찾을 수 없습니다.");
    }

    private URI buildWeatherApiUri() {
        return UriComponentsBuilder
                .fromUriString("https://f-api.github.io")
                .path("/f-api/weather.json")
                .encode()
                .build()
                .toUri();
    }

    private String getCurrentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        return LocalDate.now().format(formatter);
    }
}
