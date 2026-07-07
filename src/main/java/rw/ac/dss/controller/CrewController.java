package rw.ac.dss.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.dss.dto.request.CreateCrewRequest;
import rw.ac.dss.dto.response.CrewResponseDto;
import rw.ac.dss.service.CrewService;

import java.util.List;

@RestController
@RequestMapping("/api/crew")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;

    @PostMapping
    public ResponseEntity<CrewResponseDto> create(@Valid @RequestBody CreateCrewRequest request) {
        CrewResponseDto dto = CrewResponseDto.from(crewService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public CrewResponseDto getById(@PathVariable Long id) {
        return CrewResponseDto.from(crewService.getById(id));
    }

    @GetMapping
    public List<CrewResponseDto> list() {
        return crewService.list().stream().map(CrewResponseDto::from).toList();
    }

    @PutMapping("/{id}")
    public CrewResponseDto update(@PathVariable Long id, @Valid @RequestBody CreateCrewRequest request) {
        return CrewResponseDto.from(crewService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        crewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
