package rw.ac.dss.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.dss.client.OptimizerClient;
import rw.ac.dss.dto.optimizer.OptimizationResponseDto;
import rw.ac.dss.dto.optimizer.RecoveryOptionResultDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real controllers end-to-end (aircraft/crew/flight/delay-event creation
 * -> optimize -> select) with the Python optimizer service replaced by a mock, so the
 * test exercises RecoveryService's persistence logic without needing FastAPI running.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecoveryFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OptimizerClient optimizerClient;

    private String adminToken;

    @BeforeEach
    void login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"ChangeMe123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(body).get("token").asText();

        when(optimizerClient.optimize(any())).thenReturn(OptimizationResponseDto.builder()
                .delayEventId(1L)
                .options(List.of(
                        RecoveryOptionResultDto.builder()
                                .optionType("CREW_SUBSTITUTION")
                                .description("Substitute crew")
                                .rank(1)
                                .totalCost(400.0)
                                .crewOvertimeCost(0).fuelCost(0).passengerCompensationCost(0)
                                .slotPenaltyCost(400.0).mroCost(0)
                                .estimatedDelayReduction(120)
                                .feasible(true)
                                .feasibilityNotes("ok")
                                .generatedBy("MILP")
                                .build(),
                        RecoveryOptionResultDto.builder()
                                .optionType("FLIGHT_CANCELLATION")
                                .description("Cancel flight")
                                .rank(2)
                                .totalCost(69000.0)
                                .crewOvertimeCost(0).fuelCost(0).passengerCompensationCost(27000.0)
                                .slotPenaltyCost(0).mroCost(42000.0)
                                .estimatedDelayReduction(150)
                                .feasible(true)
                                .feasibilityNotes("last resort")
                                .generatedBy("MILP")
                                .build()
                ))
                .computationTimeMs(42.0)
                .recommendedOptionIndex(0)
                .build());
    }

    private long postAndExtractId(String uri, String body) throws Exception {
        String response = mockMvc.perform(post(uri)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void fullRecoveryFlow_fromDataEntry_toOptionSelection() throws Exception {
        long aircraftId = postAndExtractId("/api/aircraft", """
                {"registrationNumber":"9XR-TEST1","aircraftType":"BOEING_737","status":"UNSERVICEABLE",
                 "currentLocation":"KGL","totalSeats":150,"yearManufactured":2015}""");

        postAndExtractId("/api/aircraft", """
                {"registrationNumber":"9XR-TEST2","aircraftType":"BOEING_737","status":"SERVICEABLE",
                 "currentLocation":"NBO","totalSeats":150,"yearManufactured":2018}""");

        postAndExtractId("/api/crew", """
                {"firstName":"Jean","lastName":"Mugisha","employeeId":"CRW-TEST-001","role":"CAPTAIN",
                 "qualifications":"B737,A330","dutyHoursUsed":2.0,"maxDutyHours":14.0,
                 "currentLocation":"KGL","status":"AVAILABLE","overtimeRate":90.0}""");

        long flightId = postAndExtractId("/api/flights", """
                {"flightNumber":"WB999","origin":"KGL","destination":"NBO",
                 "scheduledDeparture":"2026-07-06T14:00:00","scheduledArrival":"2026-07-06T15:30:00",
                 "aircraftId":%d,"passengerCount":120,"availableSeats":150,"fuelCost":3000.0}""".formatted(aircraftId));

        long delayEventId = postAndExtractId("/api/delay-events", """
                {"flightId":%d,"delayCode":"41","delayCause":"Hydraulic fault","delayCategory":"TECHNICAL",
                 "delayMinutes":150,"reportedBy":"OCC Controller"}""".formatted(flightId));

        String optimizeResponse = mockMvc.perform(post("/api/recovery/delay-events/" + delayEventId + "/optimize")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode scenarioJson = objectMapper.readTree(optimizeResponse);
        assertThat(scenarioJson.get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(scenarioJson.get("options")).hasSize(2);
        assertThat(scenarioJson.get("options").get(0).get("optionType").asText()).isEqualTo("CREW_SUBSTITUTION");
        assertThat(scenarioJson.get("options").get(0).get("totalCost").asDouble()).isEqualTo(400.0);

        long scenarioId = scenarioJson.get("id").asLong();
        long chosenOptionId = scenarioJson.get("options").get(0).get("id").asLong();

        String getResponse = mockMvc.perform(get("/api/recovery/scenarios/" + scenarioId + "/options")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(getResponse).get("options")).hasSize(2);

        String selectResponse = mockMvc.perform(post("/api/recovery/scenarios/" + scenarioId + "/select/" + chosenOptionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionMadeBy\":\"OCC Controller Jean\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode resolved = objectMapper.readTree(selectResponse);
        assertThat(resolved.get("status").asText()).isEqualTo("RESOLVED");
        assertThat(resolved.get("selectedOptionId").asLong()).isEqualTo(chosenOptionId);
    }

    @Test
    void optimize_forUnknownDelayEvent_returns404() throws Exception {
        mockMvc.perform(post("/api/recovery/delay-events/999999/optimize")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
