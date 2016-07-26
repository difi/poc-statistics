package no.difi.statistics.ingest.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.difi.statistics.ingest.IngestService;
import no.difi.statistics.ingest.config.AppConfig;
import no.difi.statistics.model.Measurement;
import no.difi.statistics.model.TimeSeriesPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AppConfig.class, MockBackendConfig.class})
@WebAppConfiguration
public class IngestRestControllerTest {

    @Autowired
    private WebApplicationContext springContext;
    @Autowired
    private IngestService service;
    private MockMvc mockMvc;

    @Before
    public void before(){
        mockMvc = MockMvcBuilders.webAppContextSetup(springContext).build();
    }

    @Test
    public void whenSendingRequestWithValidTimeSeriesPointThenExpectValuesSentToServiceMethodToBeTheSameAsSentToService() throws Exception {
        ArgumentCaptor<TimeSeriesPoint> argumentCaptor = ArgumentCaptor.forClass(TimeSeriesPoint.class);
        String timeSeries = "test";
        TimeSeriesPoint timeSeriesPoint = createValidTimeSeriesPoint(createValidMeasurements(), ZonedDateTime.now());
        postMinutes(timeSeries, createValidJsonString(timeSeriesPoint));

        verify(service).minute(
                eq(timeSeries),
                argumentCaptor.capture()
        );

        assertCorrectValues(timeSeriesPoint, argumentCaptor);
    }

    @Test
    public void whenSendingRequestWithValidTimeSeriesPointThenExpectNormalResponse() throws Exception {
        ResultActions result = postMinutes("test", createValidJsonString(createValidTimeSeriesPoint(createValidMeasurements(), ZonedDateTime.now())));
        assertNormalResponse(result);
    }

    @Test
    public void whenSendingRequestWithInvalidJsonThenExpect400Response() throws Exception {
        ResultActions result = postMinutes("test", "invalidJson");
        assert400Response(result);
    }

    private void assert400Response(ResultActions result) throws Exception{
        result.andExpect(status().is(400));
    }

    private void assertNormalResponse(ResultActions result) throws Exception {
        result.andExpect(status().is(200));
    }

    private TimeSeriesPoint createValidTimeSeriesPoint(List<Measurement> measurements, ZonedDateTime timestamp){

        return TimeSeriesPoint.builder()
                .measurements(measurements)
                .timestamp(timestamp)
                .build();
    }

    private void assertCorrectValues(TimeSeriesPoint timeSeriesPoint, ArgumentCaptor<TimeSeriesPoint> argumentCaptor){
        assertEquals(timeSeriesPoint.getTimestamp().withZoneSameInstant(ZoneId.of("UTC")),
                argumentCaptor.getValue().getTimestamp());
        assertEquals(timeSeriesPoint.getMeasurements().get(0).getId(),
                argumentCaptor.getValue().getMeasurements().get(0).getId());
        assertEquals(timeSeriesPoint.getMeasurements().get(0).getValue(),
                argumentCaptor.getValue().getMeasurements().get(0).getValue());
    }
    private List<Measurement> createValidMeasurements(){
        List<Measurement> measurements = new ArrayList<>();
        measurements.add(new Measurement("antall", 2));
        return measurements;
    }

    private ResultActions postMinutes(String seriesName, String jsonString) throws Exception{
        String typeJson = "application/json";
        return mockMvc.perform(post("/minutes/{seriesName}", seriesName)
                .contentType(typeJson)
                .accept(typeJson)
                .content(jsonString));
    }

    private String createValidJsonString(TimeSeriesPoint timeSeriesPoint) throws Exception{
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(timeSeriesPoint);
    }

}
