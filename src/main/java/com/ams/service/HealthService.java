package com.ams.service;

import org.springframework.stereotype.Service;

import com.ams.dto.HealthResponse;

@Service
public class HealthService {

    public HealthResponse getHealthStatus() {
        return new HealthResponse("UP");
    }
}
