package com.apihub.controller;

import com.apihub.common.ApiResult;
import com.apihub.news.Countries;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/countries")
@Tag(name = "Countries", description = "Supported regions for the country explorer")
public class CountryController {

    @GetMapping
    @Operation(summary = "Countries/regions the active news provider covers")
    public ResponseEntity<ApiResult<List<Countries.Country>>> list() {
        return ResponseEntity.ok(ApiResult.ok(Countries.ALL));
    }
}
