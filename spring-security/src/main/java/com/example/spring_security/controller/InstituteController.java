package com.example.spring_security.controller;

import com.example.spring_security.model.Institute;
import com.example.spring_security.service.InstituteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/institutes") // Base path for all institute-related endpoints
public class InstituteController {

    private final InstituteService instituteService;

    @Autowired
    public InstituteController(InstituteService instituteService) {
        this.instituteService = instituteService;
    }

    /**
     * Endpoint to get all institutes.
     * Potentially useful for some scenarios, but search is preferred for autocomplete.
     * @return A list of all institutes.
     */
    @GetMapping
    public ResponseEntity<List<Institute>> getAllInstitutes() {
        List<Institute> institutes = instituteService.getAllInstitutes();
        return ResponseEntity.ok(institutes);
    }

    /**
     * Endpoint to search for institutes by a name fragment.
     * Ideal for powering an autocomplete search bar.
     * Example: GET /api/v1/institutes/search?name=Science
     * @param nameFragment The fragment of the institute name to search for.
     * @return A list of institutes matching the name fragment.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Institute>> searchInstitutes(@RequestParam("name") String nameFragment) {
        List<Institute> institutes = instituteService.searchInstitutesByName(nameFragment);
        if (institutes.isEmpty()) {
            return ResponseEntity.noContent().build(); // Return 204 No Content if no matches
        }
        return ResponseEntity.ok(institutes);
    }
} 