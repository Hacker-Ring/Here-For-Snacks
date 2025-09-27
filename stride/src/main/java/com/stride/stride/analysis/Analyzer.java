package com.stride.stride.analysis;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Analyzer {

    public Map<String, Object> analyzeRepo(String repoPath) {
        File repoDir = new File(repoPath);
        Map<String, Object> metrics = new HashMap<>();

        if (!repoDir.exists() || !repoDir.isDirectory()) {
            metrics.put("error", "Repository path does not exist or is not a directory.");
            return metrics;
        }

        int totalFiles = 0;
        int totalLines = 0;
        int maxDepth = 0;
        int totalComplexity = 0;

        Map<String, Integer> fileTypes = new HashMap<>();
        Map<String, Integer> linesPerType = new HashMap<>();
        Map<String, Integer> commentLinesPerType = new HashMap<>();
        Map<String, Double> avgLinesPerType = new HashMap<>();
        Map<String, Integer> functionsPerType = new HashMap<>();
        Map<String, Integer> classesPerType = new HashMap<>();
        Map<String, Integer> nestingDepthPerFile = new HashMap<>();
        Map<String, Double> halsteadVolumePerFile = new HashMap<>();

        List<FileStat> largestFiles = new ArrayList<>();
        List<String> optimizationFlags = new ArrayList<>();

        File[] files = repoDir.listFiles();
        if (files != null) {
            for (File f : files) {
                AdvancedResult result = analyzeFileRecursively(f, fileTypes, linesPerType,
                        commentLinesPerType, functionsPerType, classesPerType, nestingDepthPerFile,
                        halsteadVolumePerFile, largestFiles, 1, optimizationFlags);
                totalFiles += result.files;
                totalLines += result.lines;
                totalComplexity += result.complexity;
                maxDepth = Math.max(maxDepth, result.maxDepth);
            }
        }

        // Compute average lines per file type
        for (String ext : fileTypes.keySet()) {
            int filesOfType = fileTypes.get(ext);
            int linesOfType = linesPerType.getOrDefault(ext, 0);
            avgLinesPerType.put(ext, filesOfType > 0 ? (double) linesOfType / filesOfType : 0);
        }

        // Sort top 5 largest files
        largestFiles.sort((a, b) -> Integer.compare(b.lines, a.lines));
        List<String> top5Largest = new ArrayList<>();
        for (int i = 0; i < Math.min(5, largestFiles.size()); i++) {
            top5Largest.add(largestFiles.get(i).path + " (" + largestFiles.get(i).lines + " lines)");
            if (largestFiles.get(i).lines > 500) { // large file threshold
                optimizationFlags.add("File " + largestFiles.get(i).path + " exceeds 500 lines");
            }
        }

        metrics.put("totalFiles", totalFiles);
        metrics.put("totalLines", totalLines);
        metrics.put("fileTypes", fileTypes);
        metrics.put("linesPerType", linesPerType);
        metrics.put("commentLinesPerType", commentLinesPerType);
        metrics.put("avgLinesPerType", avgLinesPerType);
        metrics.put("cyclomaticComplexity", totalComplexity);
        metrics.put("maxDepth", maxDepth);
        metrics.put("top5LargestFiles", top5Largest);
        metrics.put("functionsPerType", functionsPerType);
        metrics.put("classesPerType", classesPerType);
        metrics.put("nestingDepthPerFile", nestingDepthPerFile);
        metrics.put("halsteadVolumePerFile", halsteadVolumePerFile);
        metrics.put("optimizationFlags", optimizationFlags);

        return metrics;
    }

    private AdvancedResult analyzeFileRecursively(File file,
                                                   Map<String, Integer> fileTypes,
                                                   Map<String, Integer> linesPerType,
                                                   Map<String, Integer> commentLinesPerType,
                                                   Map<String, Integer> functionsPerType,
                                                   Map<String, Integer> classesPerType,
                                                   Map<String, Integer> nestingDepthPerFile,
                                                   Map<String, Double> halsteadVolumePerFile,
                                                   List<FileStat> largestFiles,
                                                   int depth,
                                                   List<String> optimizationFlags) {
        int filesCount = 0;
        int linesCount = 0;
        int complexity = 0;
        int maxDepth = depth;

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    AdvancedResult childResult = analyzeFileRecursively(child, fileTypes, linesPerType,
                            commentLinesPerType, functionsPerType, classesPerType, nestingDepthPerFile,
                            halsteadVolumePerFile, largestFiles, depth + 1, optimizationFlags);
                    filesCount += childResult.files;
                    linesCount += childResult.lines;
                    complexity += childResult.complexity;
                    maxDepth = Math.max(maxDepth, childResult.maxDepth);
                }
            }
        } else {
            filesCount++;
            String ext = getFileExtension(file.getName());
            fileTypes.put(ext, fileTypes.getOrDefault(ext, 0) + 1);

            int fileLines = 0;
            int fileComments = 0;
            int fileComplexity = 1;
            int functionCount = 0;
            int classCount = 0;
            //int nestingDepth = 0;

            int maxNesting = 0;
            int currentNesting = 0;
            int operators = 0;
            int operands = 0;

            Pattern functionPattern = Pattern.compile(
                    "(public|private|protected)?\\s*(static)?\\s*\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{?");
            Pattern classPattern = Pattern.compile("(class|interface|enum)\\s+\\w+");
            Pattern todoPattern = Pattern.compile(".*(TODO|FIXME).*");

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    fileLines++;
                    line = line.trim();

                    // Comments
                    if (line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) fileComments++;

                    // Cyclomatic complexity
                    if (line.matches(".*\\b(if|for|while|case|catch|switch)\\b.*")) fileComplexity++;

                    // Functions
                    if (functionPattern.matcher(line).find()) functionCount++;

                    // Classes
                    if (classPattern.matcher(line).find()) classCount++;

                    // Nesting
                    if (line.contains("{")) currentNesting++;
                    if (line.contains("}")) currentNesting--;
                    maxNesting = Math.max(maxNesting, currentNesting);

                    // Halstead approximation
                    String[] tokens = line.split("\\W+");
                    for (String token : tokens) {
                        if (token.isEmpty()) continue;
                        if (isOperator(token)) operators++;
                        else operands++;
                    }

                    if (todoPattern.matcher(line).matches()) {
                        optimizationFlags.add("Technical debt in " + file.getAbsolutePath() + ": " + line);
                    }
                }
            } catch (Exception ignored) {}

            linesCount += fileLines;
            complexity += fileComplexity;
            functionsPerType.put(ext, functionsPerType.getOrDefault(ext, 0) + functionCount);
            classesPerType.put(ext, classesPerType.getOrDefault(ext, 0) + classCount);
            nestingDepthPerFile.put(file.getAbsolutePath(), maxNesting);

            double halsteadVolume = (operators + operands) * Math.log(Math.max(operands,1)) / Math.log(2);
            halsteadVolumePerFile.put(file.getAbsolutePath(), halsteadVolume);

            linesPerType.put(ext, linesPerType.getOrDefault(ext, 0) + fileLines);
            commentLinesPerType.put(ext, commentLinesPerType.getOrDefault(ext, 0) + fileComments);

            largestFiles.add(new FileStat(file.getAbsolutePath(), fileLines));
        }

        return new AdvancedResult(filesCount, linesCount, complexity, maxDepth);
    }

    private boolean isOperator(String token) {
        String ops = "+-*/%=!<>|&^~";
        return token.length() == 1 && ops.contains(token);
    }

    private String getFileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0 && idx < fileName.length() - 1) return fileName.substring(idx + 1);
        return "no_extension";
    }

    private static class AdvancedResult {
        int files;
        int lines;
        int complexity;
        int maxDepth;

        AdvancedResult(int files, int lines, int complexity, int maxDepth) {
            this.files = files;
            this.lines = lines;
            this.complexity = complexity;
            this.maxDepth = maxDepth;
        }
    }

    private static class FileStat {
        String path;
        int lines;

        FileStat(String path, int lines) {
            this.path = path;
            this.lines = lines;
        }
    }
}
