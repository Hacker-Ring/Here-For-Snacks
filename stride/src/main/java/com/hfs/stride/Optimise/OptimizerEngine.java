package com.hfs.stride.Optimise;

import com.stride.stride.model.AnalysisResult;
import com.stride.stride.model.OptimizationResult;

import java.util.ArrayList;
import java.util.List;

public class OptimizerEngine {

    public OptimizationResult optimize(AnalysisResult analysis) {
        List<String> suggestions = new ArrayList<>();

        if (analysis.getTotalFiles() > 1000) {
            suggestions.add("Repository has too many files. Consider modularization.");
        }

        if (analysis.getJavaFiles() == 0) {
            suggestions.add("No Java files detected. Add support for other languages.");
        }

        if (analysis.getTotalLines() > 50000) {
            suggestions.add("Codebase is very large. Break down into sub-projects or microservices.");
        }

        if (analysis.getJavaFiles() > 0) {
            double avgLinesPerFile = (double) analysis.getTotalLines() / analysis.getJavaFiles();
            if (avgLinesPerFile > 500) {
                suggestions.add("High average lines per file (" + avgLinesPerFile + "). Consider splitting large classes.");
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Repository looks well-structured. No immediate optimizations needed.");
        }

        return new OptimizationResult(suggestions);
    }
}
