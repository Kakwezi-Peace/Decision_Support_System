package rw.ac.dss.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.dss.dto.request.CreateDelayEventRequest;
import rw.ac.dss.dto.response.DelayEventResponseDto;
import rw.ac.dss.service.DelayEventService;

import java.util.List;

@RestController
@RequestMapping("/api/delay-events")
@RequiredArgsConstructor
public class DelayEventController {

    private final DelayEventService delayEventService;

    @PostMapping
    public ResponseEntity<DelayEventResponseDto> create(@Valid @RequestBody CreateDelayEventRequest request) {
        DelayEventResponseDto dto = DelayEventResponseDto.from(delayEventService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public DelayEventResponseDto getById(@PathVariable Long id) {
        return DelayEventResponseDto.from(delayEventService.getById(id));
    }

    @GetMapping
    public List<DelayEventResponseDto> list() {
        return delayEventService.list().stream().map(DelayEventResponseDto::from).toList();
    }

    @PutMapping("/{id}")
    public DelayEventResponseDto update(@PathVariable Long id, @Valid @RequestBody CreateDelayEventRequest request) {
        return DelayEventResponseDto.from(delayEventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        delayEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
