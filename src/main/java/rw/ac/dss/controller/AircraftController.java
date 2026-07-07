package rw.ac.dss.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.dss.dto.request.CreateAircraftRequest;
import rw.ac.dss.dto.response.AircraftResponseDto;
import rw.ac.dss.service.AircraftService;

import java.util.List;

@RestController
@RequestMapping("/api/aircraft")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftService aircraftService;

    @PostMapping
    public ResponseEntity<AircraftResponseDto> create(@Valid @RequestBody CreateAircraftRequest request) {
        AircraftResponseDto dto = AircraftResponseDto.from(aircraftService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public AircraftResponseDto getById(@PathVariable Long id) {
        return AircraftResponseDto.from(aircraftService.getById(id));
    }

    @GetMapping
    public List<AircraftResponseDto> list() {
        return aircraftService.list().stream().map(AircraftResponseDto::from).toList();
    }

    @PutMapping("/{id}")
    public AircraftResponseDto update(@PathVariable Long id, @Valid @RequestBody CreateAircraftRequest request) {
        return AircraftResponseDto.from(aircraftService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        aircraftService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
