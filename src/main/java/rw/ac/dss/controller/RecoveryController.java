package rw.ac.dss.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.dss.dto.request.CreateManualOptionRequest;
import rw.ac.dss.dto.request.RecordOutcomeRequest;
import rw.ac.dss.dto.request.SelectOptionRequest;
import rw.ac.dss.dto.response.RecoveryScenarioResponseDto;
import rw.ac.dss.dto.response.RecoveryScenarioSummaryDto;
import rw.ac.dss.service.RecoveryService;

import java.util.List;

@RestController
@RequestMapping("/api/recovery")
@RequiredArgsConstructor
public class RecoveryController {

    private final RecoveryService recoveryService;

    @GetMapping("/scenarios")
    public List<RecoveryScenarioSummaryDto> listScenarios() {
        return recoveryService.listScenarios();
    }

    @PostMapping("/delay-events/{delayEventId}/optimize")
    public RecoveryScenarioResponseDto optimize(@PathVariable Long delayEventId) {
        return recoveryService.generateOptions(delayEventId);
    }

    @GetMapping("/delay-events/{delayEventId}/scenario")
    public ResponseEntity<RecoveryScenarioResponseDto> getScenarioForDelayEvent(@PathVariable Long delayEventId) {
        return recoveryService.findScenarioForDelayEvent(delayEventId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/scenarios/{scenarioId}/options")
    public RecoveryScenarioResponseDto getScenario(@PathVariable Long scenarioId) {
        return recoveryService.getScenario(scenarioId);
    }

    @PostMapping("/scenarios/{scenarioId}/select/{optionId}")
    public RecoveryScenarioResponseDto selectOption(@PathVariable Long scenarioId,
                                                      @PathVariable Long optionId,
                                                      @RequestBody(required = false) SelectOptionRequest request) {
        String decisionMadeBy = request != null ? request.getDecisionMadeBy() : null;
        return recoveryService.selectOption(scenarioId, optionId, decisionMadeBy);
    }

    @PostMapping("/scenarios/{scenarioId}/manual-option")
    public ResponseEntity<RecoveryScenarioResponseDto> addManualOption(@PathVariable Long scenarioId,
                                                                        @Valid @RequestBody CreateManualOptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recoveryService.addManualOption(scenarioId, request));
    }

    @PostMapping("/scenarios/{scenarioId}/outcome")
    public RecoveryScenarioResponseDto recordOutcome(@PathVariable Long scenarioId,
                                                       @Valid @RequestBody RecordOutcomeRequest request) {
        return recoveryService.recordOutcome(scenarioId, request.getActualCost(), request.getManualBaselineCost());
    }
}
