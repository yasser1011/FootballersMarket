package com.yasser.footballersmarket.fixture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// the dto is read straight from api json, so these pin the shapes the api really sends
class FixtureResponseDtoTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    @Test
    void shouldReadMinutesRatingAndTeamOfEachPlayersBlock() throws Exception {
        String json = """
                {"errors": [], "response": [{
                  "fixture": {"id": 99, "status": {"short": "FT"}},
                  "players": [{
                    "team": {"id": 42, "name": "Arsenal"},
                    "players": [
                      {"player": {"id": 1, "name": "Saka"},
                       "statistics": [{"games": {"minutes": 90, "rating": "7.9"}, "goals": {"total": 1, "assists": 0}}]},
                      {"player": {"id": 2, "name": "Sub"},
                       "statistics": [{"games": {"minutes": 0, "rating": "0"}, "goals": {"total": null, "assists": null}}]}
                    ]}]
                }]}
                """;

        FixtureResponseDto dto = objectMapper.readValue(json, FixtureResponseDto.class);

        FixtureResponseDto.Response.TeamPlayers block = dto.response()[0].players().get(0);
        assertThat(block.team().id()).isEqualTo(42L);
        assertThat(block.players()).hasSize(2);
        assertThat(block.players().get(0).statistics().get(0).games().minutes()).isEqualTo(90);
        assertThat(block.players().get(0).statistics().get(0).games().rating()).isEqualTo("7.9");
        assertThat(block.players().get(1).statistics().get(0).games().minutes()).isZero();
    }

    @Test
    void shouldSeeNoErrorsWhenTheApiSendsAnEmptyArray() throws Exception {
        FixtureResponseDto dto = objectMapper.readValue("{\"errors\": [], \"response\": []}", FixtureResponseDto.class);

        assertThat(dto.errors().size()).isZero();
    }

    @Test
    void shouldSeeErrorsWhenTheApiSendsAnObject() throws Exception {
        // the api rejects a bad request with HTTP 200 and an object of messages
        FixtureResponseDto dto = objectMapper.readValue(
                "{\"errors\": {\"ids\": \"too many ids\"}, \"response\": []}", FixtureResponseDto.class);

        assertThat(dto.errors().size()).isPositive();
    }
}
