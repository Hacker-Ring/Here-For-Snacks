package com.stride.stride.optimizer;

import java.util.*;
import java.util.stream.Collectors;

public class OptimizerEngine {

    /**
     * Generate highly advanced optimization suggestions
     * @param metrics Map from Analyzer
     * @return List of actionable suggestions
     */
    public List<String> generateSuggestions(Map<String, Object> metrics) {
        List<String> suggestions = new ArrayList<>();

        if (metrics.containsKey("error")) {
            suggestions.add("Cannot optimize: " + metrics.get("error"));
            return suggestions;
        }

        int totalFiles = (int) metrics.getOrDefault("totalFiles", 0);
        int totalLines = (int) metrics.getOrDefault("totalLines", 0);
        int complexity = (int) metrics.getOrDefault("cyclomaticComplexity", 0);
        int maxDepth = (int) metrics.getOrDefault("maxDepth", 0);

        Map<String, Integer> fileTypes = castMap(metrics.getOrDefault("fileTypes", new HashMap<>()));
        Map<String, Integer> linesPerType = castMap(metrics.getOrDefault("linesPerType", new HashMap<>()));
        Map<String, Integer> commentLines = castMap(metrics.getOrDefault("commentLinesPerType", new HashMap<>()));
        Map<String, Integer> functionsPerType = castMap(metrics.getOrDefault("functionsPerType", new HashMap<>()));
        Map<String, Integer> classesPerType = castMap(metrics.getOrDefault("classesPerType", new HashMap<>()));
        Map<String, Integer> nestingDepth = castMap(metrics.getOrDefault("nestingDepthPerFile", new HashMap<>()));
        Map<String, Double> halsteadVolume = castDoubleMap(metrics.getOrDefault("halsteadVolumePerFile", new HashMap<>()));
        List<String> optimizationFlags = castList(metrics.getOrDefault("optimizationFlags", new ArrayList<>()));

        Map<String, Double> severityScores = new HashMap<>();

        // 1. Compute severity per file using weighted complexity
        for (String file : nestingDepth.keySet()) {
            double severity = 0;
            int nest = nestingDepth.getOrDefault(file, 0);
            double hal = halsteadVolume.getOrDefault(file, 0.0);
            severity = complexity * 0.4 + hal * 0.3 + nest * 0.3;
            severityScores.put(file, severity);

            if (severity > 1000) {
                suggestions.add("[HIGH PRIORITY] Refactor file: " + file + " (Severity=" + String.format("%.1f", severity) + ")");
            }
        }

        // 2. Large files and functions
        List<String> largeFiles = ((List<String>) metrics.getOrDefault("top5LargestFiles", new ArrayList<>()));
        for (String lf : largeFiles) {
            suggestions.add("[LARGE FILE] Review: " + lf);
        }

        // 3. Documentation gaps
        for (String ext : fileTypes.keySet()) {
            int totalLinesOfType = linesPerType.getOrDefault(ext, 0);
            int comments = commentLines.getOrDefault(ext, 0);
            double commentRatio = totalLinesOfType > 0 ? ((double) comments / totalLinesOfType) * 100 : 0;
            if (commentRatio < 50) {
                suggestions.add("[DOC GAP] Low documentation in ." + ext + " files (" + String.format("%.1f", commentRatio) + "% comments)");
            }
        }

        // 4. Function/Class cohesion
        for (String ext : functionsPerType.keySet()) {
            int funcs = functionsPerType.getOrDefault(ext, 0);
            int classes = classesPerType.getOrDefault(ext, 1); // avoid div by zero
            double avgFunctionsPerClass = (double) funcs / classes;
            if (avgFunctionsPerClass > 10) {
                suggestions.add("[COHESION] High functions per class in ." + ext + " (" + String.format("%.1f", avgFunctionsPerClass) + ")");
            }
        }

        // 5. Deep nesting warnings
        nestingDepth.entrySet().stream()
                .filter(e -> e.getValue() > 5)
                .forEach(e -> suggestions.add("[NESTING] Deep nesting in file " + e.getKey() + " (" + e.getValue() + ")"));

        // 6. Technical debt
        suggestions.addAll(optimizationFlags);

        // 7. Directory depth warnings
        if (maxDepth > 10) {
            suggestions.add("[STRUCTURE] Repository directory depth is " + maxDepth + "; consider flattening modules");
        }

        // 8. Overall repo size advice
        if (totalFiles > 500) {
            suggestions.add("[MODULARIZE] High file count (" + totalFiles + "); consider splitting into submodules");
        }
        if (totalLines > 20000) {
            suggestions.add("[REFACTOR] Large codebase (" + totalLines + " lines); review large classes/functions");
        }

        // 9. Dominant file type
        String dominantType = fileTypes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
        suggestions.add("[INFO] Dominant file type: " + dominantType);

        // 10. Halstead complexity per file
        halsteadVolume.entrySet().stream()
                .filter(e -> e.getValue() > 5000)
                .forEach(e -> suggestions.add("[HALSTEAD] High complexity in file " + e.getKey() + " (Volume=" + String.format("%.0f", e.getValue()) + ")"));

        // 11. Unused/empty files
        List<String> zeroLineFiles = linesPerType.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (!zeroLineFiles.isEmpty()) {
            suggestions.add("[CLEANUP] Some file types have zero lines: " + zeroLineFiles);
        }

        return suggestions;
    }

    // Utility casts
    @SuppressWarnings("unchecked")
    private Map<String,Integer> castMap(Object obj) {
        return (Map<String,Integer>) obj;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Double> castDoubleMap(Object obj) {
        return (Map<String,Double>) obj;
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object obj) {
        return (List<String>) obj;
    }
}
