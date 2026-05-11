package org.example.expert.domain.client;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.exception.ServerException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(WeatherClient.class) // RestTemplate 관련 테스트 전용 어노테이션
class WeatherClientTest {

    @Autowired
    private WeatherClient weatherClient;

    @Autowired
    private MockRestServiceServer mockServer; // 가짜 서버 역할을 해줍니다.

    @Test
    void 오늘_날씨를_성공적으로_가져온다() {
        // given
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        String mockResponse = "[{\"date\":\"" + today + "\",\"weather\":\"Sunny\"}]";

        mockServer.expect(requestTo("https://f-api.github.io/f-api/weather.json"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        // when
        String weather = weatherClient.getTodayWeather();

        // then
        assertEquals("Sunny", weather);
    }

    @Test
    void 외부_API_에러_발생_시_ServerException으로_변환된다() {
        // given: 500 에러 응답 설정
        mockServer.expect(requestTo("https://f-api.github.io/f-api/weather.json"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then: 이제 try-catch에서 가로채서 ServerException을 던집니다.
        assertThrows(ServerException.class, () -> weatherClient.getTodayWeather());
    }

    @Test
    void 오늘_날짜의_데이터가_없으면_ServerException이_발생한다() {
        // given: 오늘이 아닌 다른 날짜 데이터만 응답
        String mockResponse = "[{\"date\":\"12-31\",\"weather\":\"Snow\"}]";

        mockServer.expect(requestTo("https://f-api.github.io/f-api/weather.json"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        // when & then
        ServerException exception = assertThrows(ServerException.class, () ->
                weatherClient.getTodayWeather()
        );
        assertEquals("오늘에 해당하는 날씨 데이터를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 응답_데이터가_비어있으면_ServerException이_발생한다() {
        // given
        mockServer.expect(requestTo("https://f-api.github.io/f-api/weather.json"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        // when & then
        ServerException exception = assertThrows(ServerException.class, () ->
                weatherClient.getTodayWeather()
        );
        assertEquals("날씨 데이터가 없습니다.", exception.getMessage());
    }
}