package rw.ac.dss.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.dss.dto.request.CreateFlightRequest;
import rw.ac.dss.dto.response.FlightResponseDto;
import rw.ac.dss.service.FlightService;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    public ResponseEntity<FlightResponseDto> create(@Valid @RequestBody CreateFlightRequest request) {
        FlightResponseDto dto = FlightResponseDto.from(flightService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public FlightResponseDto getById(@PathVariable Long id) {
        return FlightResponseDto.from(flightService.getById(id));
    }

    @GetMapping
    public List<FlightResponseDto> list() {
        return flightService.list().stream().map(FlightResponseDto::from).toList();
    }

    @PutMapping("/{id}")
    public FlightResponseDto update(@PathVariable Long id, @Valid @RequestBody CreateFlightRequest request) {
        return FlightResponseDto.from(flightService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flightService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
