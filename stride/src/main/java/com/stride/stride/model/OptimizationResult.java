package com.stride.stride.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OptimizationResult {
    private List<String> suggestions;
}
