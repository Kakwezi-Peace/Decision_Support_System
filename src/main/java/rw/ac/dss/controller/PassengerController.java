package rw.ac.dss.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.dss.dto.request.CreatePassengerRequest;
import rw.ac.dss.dto.response.PassengerResponseDto;
import rw.ac.dss.service.PassengerService;

import java.util.List;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PostMapping
    public ResponseEntity<PassengerResponseDto> create(@Valid @RequestBody CreatePassengerRequest request) {
        PassengerResponseDto dto = PassengerResponseDto.from(passengerService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public PassengerResponseDto getById(@PathVariable Long id) {
        return PassengerResponseDto.from(passengerService.getById(id));
    }

    @GetMapping
    public List<PassengerResponseDto> list() {
        return passengerService.list().stream().map(PassengerResponseDto::from).toList();
    }

    @PutMapping("/{id}")
    public PassengerResponseDto update(@PathVariable Long id, @Valid @RequestBody CreatePassengerRequest request) {
        return PassengerResponseDto.from(passengerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        passengerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
