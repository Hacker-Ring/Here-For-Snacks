package com.stride.stride.analysis;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

/**
 * Advanced Analyzer - preserves return type Map<String,Object> for compatibility.
 * Adds maintainability, duplication detection, coupling, secrets detection, git churn (optional), cognitive complexity approx.
 */
public class Analyzer {

    // Configurable defaults (can be overridden by stride-analyzer.properties in repo root)
    private static final int DUP_WINDOW_TOKENS_DEFAULT = 50;
    private static final int LARGE_FILE_THRESHOLD_DEFAULT = 500;
    private static final int COMPLEXITY_METHOD_THRESHOLD_DEFAULT = 10;

    private Properties config = new Properties();

    public Analyzer() {
        // defaults can remain; actual config loaded per-repo if file present.
    }

    public Map<String, Object> analyzeRepo(String repoPath) {
        // Load repo-specific config if present
        loadRepoConfig(repoPath);

        File repoDir = new File(repoPath);
        Map<String, Object> metrics = new HashMap<>();

        if (!repoDir.exists() || !repoDir.isDirectory()) {
            metrics.put("error", "Repository path does not exist or is not a directory.");
            return metrics;
        }

        // Primary aggregation values (existing)
        int totalFiles = 0;
        int totalLines = 0;
        int maxDepth = 0;
        int totalComplexity = 0;

        // Per-type maps (existing + extended)
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
        List<String> secretsFound = new ArrayList<>();

        // New metrics
        Map<String, Double> commentDensityPerFile = new HashMap<>();
        Map<String, Integer> todoCountPerFile = new HashMap<>();
        Map<String, Integer> cognitiveComplexityPerFile = new HashMap<>();
        Map<String, Integer> fileCoupling = new HashMap<>(); // imports/require counts per file

        // Duplicate detection helper: store per-file token list hashed across sliding windows
        Map<String, List<String>> fileTokenHashes = new HashMap<>();

        int dupWindow = getIntConfig("dup.window.tokens", DUP_WINDOW_TOKENS_DEFAULT);
        int largeFileThreshold = getIntConfig("large.file.threshold", LARGE_FILE_THRESHOLD_DEFAULT);

        File[] files = repoDir.listFiles();
        if (files != null) {
            for (File f : files) {
                AdvancedResult result = analyzeFileRecursively(f, fileTypes, linesPerType,
                        commentLinesPerType, functionsPerType, classesPerType, nestingDepthPerFile,
                        halsteadVolumePerFile, largestFiles, 1, optimizationFlags,
                        commentDensityPerFile, todoCountPerFile, cognitiveComplexityPerFile,
                        fileCoupling, fileTokenHashes, secretsFound, dupWindow);
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
            if (largestFiles.get(i).lines > largeFileThreshold) { // large file threshold
                optimizationFlags.add("File " + largestFiles.get(i).path + " exceeds " + largeFileThreshold + " lines");
            }
        }

        // Duplicate detection summary
        List<String> duplicateBlocks = detectDuplicateBlocks(fileTokenHashes);

        // Maintainability Index (aggregate)
        double avgHalstead = average(halsteadVolumePerFile.values());
        double maintainabilityIndex = computeMaintainabilityIndex(totalLines, totalComplexity, avgHalstead);

        // Comment density aggregates
        double overallCommentDensity = computeOverallCommentDensity(commentLinesPerType, linesPerType);

        // Coupling hotspots (top 10 by import usage)
        List<Map.Entry<String, Integer>> couplingList = new ArrayList<>(fileCoupling.entrySet());
        couplingList.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> topCoupledFiles = new ArrayList<>();
        for (int i = 0; i < Math.min(10, couplingList.size()); i++) {
            topCoupledFiles.add(couplingList.get(i).getKey() + " (" + couplingList.get(i).getValue() + ")");
        }

        
        // Git churn if repo is git (optional)
        Map<String, Integer> gitChurn = new HashMap<>();
        try {
            gitChurn = computeGitChurn(repoDir);
        } catch (Exception ignored) {
            // if git is unavailable, just leave empty - non-fatal
        }
        
        // Put everything in metrics map (preserve existing keys)
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

        // New keys (backwards-compatible - added)
        metrics.put("maintainabilityIndex", maintainabilityIndex);
        metrics.put("avgHalsteadVolume", avgHalstead);
        metrics.put("commentDensityPerFile", commentDensityPerFile);
        metrics.put("overallCommentDensity", overallCommentDensity);
        metrics.put("todoCountPerFile", todoCountPerFile);
        metrics.put("duplicateBlocks", duplicateBlocks);
        metrics.put("secretsFound", secretsFound);
        metrics.put("cognitiveComplexityPerFile", cognitiveComplexityPerFile);
        metrics.put("fileCoupling", fileCoupling);
        metrics.put("topCoupledFiles", topCoupledFiles);
        metrics.put("gitChurnPerFile", gitChurn);
        
        return metrics;
    }

    private void loadRepoConfig(String repoPath) {
        File cfg = new File(repoPath, "stride-analyzer.properties");
        if (cfg.exists()) {
            try (FileInputStream fis = new FileInputStream(cfg)) {
                config.load(fis);
            } catch (IOException ignored) {}
        }
    }

    private int getIntConfig(String key, int def) {
        String v = config.getProperty(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private double average(Collection<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        double s = 0.0;
        for (Double d : values) s += d;
        return s / values.size();
    }

    private double computeOverallCommentDensity(Map<String, Integer> commentLinesPerType, Map<String, Integer> linesPerType) {
        int totalComments = 0;
        int totalLines = 0;
        for (String k : linesPerType.keySet()) {
            totalLines += linesPerType.getOrDefault(k, 0);
            totalComments += commentLinesPerType.getOrDefault(k, 0);
        }
        if (totalLines == 0) return 0.0;
        return (double) totalComments / totalLines;
    }

    /**
     * Compute Maintainability Index using common formula.
     * MI = 171 - 5.2 * ln(H) - 0.23 * C - 16.2 * ln(LOC)
     * Where H is Halstead Volume, C is cyclomatic complexity, LOC lines of code
     */
    private double computeMaintainabilityIndex(int loc, int cyclo, double halsteadVolume) {
        double H = Math.max(halsteadVolume, 1.0);
        double LOC = Math.max(loc, 1.0);
        double mi = 171.0 - 5.2 * Math.log(H) - 0.23 * cyclo - 16.2 * Math.log(LOC);
        // normalize roughly to 0..100
        if (mi < 0) mi = 0;
        if (mi > 100) mi = Math.min(mi, 100);
        return Math.round(mi * 100.0) / 100.0;
    }

    private List<String> detectDuplicateBlocks(Map<String, List<String>> fileTokenHashes) {
        // fileTokenHashes: path -> list of window-hashes
        // naive cross-file duplicate detection: find identical hashes across files
        Map<String, List<String>> hashToFiles = new HashMap<>();
        for (Map.Entry<String, List<String>> e : fileTokenHashes.entrySet()) {
            String file = e.getKey();
            for (String h : e.getValue()) {
                hashToFiles.computeIfAbsent(h, k -> new ArrayList<>()).add(file);
            }
        }
        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : hashToFiles.entrySet()) {
            List<String> files = e.getValue();
            if (files.size() > 1) {
                // Group duplicates (collapse to unique pairs)
                Set<String> uniq = new HashSet<>(files);
                duplicates.add("Duplicate block across: " + String.join(", ", uniq));
            }
        }
        return duplicates;
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
                                                   List<String> optimizationFlags,
                                                   Map<String, Double> commentDensityPerFile,
                                                   Map<String, Integer> todoCountPerFile,
                                                   Map<String, Integer> cognitiveComplexityPerFile,
                                                   Map<String, Integer> fileCoupling,
                                                   Map<String, List<String>> fileTokenHashes,
                                                   List<String> secretsFound,
                                                   int dupWindowTokens) {
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
                            halsteadVolumePerFile, largestFiles, depth + 1, optimizationFlags,
                            commentDensityPerFile, todoCountPerFile, cognitiveComplexityPerFile,
                            fileCoupling, fileTokenHashes, secretsFound, dupWindowTokens);
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

            int maxNesting = 0;
            int currentNesting = 0;
            int operators = 0;
            int operands = 0;

            int cognitiveComplexity = 0; // approximation
            int todoCount = 0;
            int importCount = 0;

            List<String> tokenWindowHashes = new ArrayList<>();

            Pattern functionPattern = Pattern.compile(
                    "(public|private|protected)?\\s*(static\\s+)?[\\w\\<\\>\\[\\]]+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{?");
            Pattern classPattern = Pattern.compile("(class|interface|enum)\\s+\\w+");
            Pattern todoPattern = Pattern.compile(".*(TODO|FIXME).*", Pattern.CASE_INSENSITIVE);
            Pattern importPattern = Pattern.compile("^(import\\s+|#include\\s+|require\\(|from\\s+\\S+\\s+import).*");
            Pattern secretPattern1 = Pattern.compile("(?i)(apikey|api_key|secret|token|passwd|password)\\s*[=:\"']\\s*[^\\s\"']+");
            Pattern secretPattern2 = Pattern.compile("(?i)(AKIA|AIza|SG\\.|-----BEGIN PRIVATE KEY-----|xoxp-|xoxb-)[A-Za-z0-9\\-_=+/]{16,}");
            Pattern base64Likely = Pattern.compile("\\b([A-Za-z0-9+/]{40,}={0,2})\\b");

            List<String> tokenBuffer = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    fileLines++;
                    String trimmed = line.trim();

                    // Comments
                    if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("#")) {
                        fileComments++;
                    }

                    // TODOs
                    if (todoPattern.matcher(line).find()) {
                        todoCount++;
                    }

                    // Cyclomatic complexity (if/for/while/case/catch/switch/&&/||/?:)
                    if (trimmed.matches(".*\\b(if|for|while|case|catch|switch)\\b.*")) fileComplexity++;

                    // boolean operators increase cognitive weight
                    if (trimmed.contains("&&") || trimmed.contains("||") || trimmed.contains("?") || trimmed.contains(":")) {
                        cognitiveComplexity += 1;
                    }

                    // Functions & classes
                    if (functionPattern.matcher(trimmed).find()) {
                        functionCount++;
                        // heuristics: if function contains multiple control-flow keywords later, bump cognitive
                    }
                    if (classPattern.matcher(trimmed).find()) classCount++;

                    // Nesting/cognitive (approx): opening braces increase nesting weight
                    if (trimmed.contains("{")) {
                        currentNesting++;
                        cognitiveComplexity += Math.max(1, currentNesting / 2); // deeper nesting increases cognitive cost
                    }
                    if (trimmed.contains("}")) {
                        currentNesting = Math.max(0, currentNesting - 1);
                    }
                    maxNesting = Math.max(maxNesting, currentNesting);

                    // Halstead approximation tokens
                    String[] tokens = trimmed.split("\\W+");
                    for (String token : tokens) {
                        if (token.isEmpty()) continue;
                        if (isOperator(token)) operators++;
                        else operands++;
                        tokenBuffer.add(token);
                        if (tokenBuffer.size() >= dupWindowTokens) {
                            String h = hashTokens(tokenBuffer);
                            tokenWindowHashes.add(h);
                            // slide by half the window to reduce sensitivity
                            int slide = Math.max(1, dupWindowTokens / 2);
                            for (int s = 0; s < slide && !tokenBuffer.isEmpty(); s++) tokenBuffer.remove(0);
                        }
                    }

                    // Coupling: imports/require/includes
                    if (importPattern.matcher(trimmed).find()) importCount++;

                    // Secret heuristics
                    if (secretPattern1.matcher(line).find() || secretPattern2.matcher(line).find() || base64Likely.matcher(line).find()) {
                        secretsFound.add("Potential secret in " + file.getAbsolutePath() + ":" + fileLines + " -> " + line.trim());
                    }
                }

                // flush leftover token buffer possible small windows (optional)
                if (!tokenBuffer.isEmpty() && tokenBuffer.size() >= Math.max(4, dupWindowTokens / 4)) {
                    tokenWindowHashes.add(hashTokens(tokenBuffer));
                }

            } catch (Exception ignored) {}

            linesCount += fileLines;
            complexity += fileComplexity;
            functionsPerType.put(ext, functionsPerType.getOrDefault(ext, 0) + functionCount);
            classesPerType.put(ext, classesPerType.getOrDefault(ext, 0) + classCount);
            nestingDepthPerFile.put(file.getAbsolutePath(), maxNesting);

            double halsteadVolume = (operators + operands) * Math.log(Math.max(operands, 1)) / Math.log(2);
            halsteadVolumePerFile.put(file.getAbsolutePath(), halsteadVolume);

            linesPerType.put(ext, linesPerType.getOrDefault(ext, 0) + fileLines);
            commentLinesPerType.put(ext, commentLinesPerType.getOrDefault(ext, 0) + fileComments);

            largestFiles.add(new FileStat(file.getAbsolutePath(), fileLines));

            // New aggregates
            double cDensity = fileLines == 0 ? 0.0 : ((double) fileComments / fileLines);
            commentDensityPerFile.put(file.getAbsolutePath(), Math.round(cDensity * 10000.0) / 10000.0);
            todoCountPerFile.put(file.getAbsolutePath(), todoCount);
            cognitiveComplexityPerFile.put(file.getAbsolutePath(), cognitiveComplexity);
            fileCoupling.put(file.getAbsolutePath(), importCount);
            fileTokenHashes.put(file.getAbsolutePath(), tokenWindowHashes);
        }

        return new AdvancedResult(filesCount, linesCount, complexity, maxDepth);
    }

    private String hashTokens(List<String> tokens) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (String t : tokens) md.update(t.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            // fallback simple join
            return String.join("-", tokens).hashCode() + "";
        }
    }

    private boolean isOperator(String token) {
        // consider multi-char operators as tokens too
        String ops = "+-*/%=!<>|&^~";
        if (token.length() == 1 && ops.contains(token)) return true;
        // heuristics for operator-like tokens
        return token.matches("==|!=|<=|>=|&&|\\|\\||\\+\\+|--|->|::");
    }

    private String getFileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0 && idx < fileName.length() - 1) return fileName.substring(idx + 1).toLowerCase();
        return "no_extension";
    }

    private Map<String, Integer> computeGitChurn(File repoDir) throws IOException, InterruptedException {
        Map<String, Integer> churn = new HashMap<>();
        File gitDir = new File(repoDir, ".git");
        if (!gitDir.exists()) return churn; // not a git repo
        // run: git log --pretty=format: --name-only
        ProcessBuilder pb = new ProcessBuilder("git", "log", "--pretty=format:", "--name-only");
        pb.directory(repoDir);
        Process p = pb.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l;
            while ((l = br.readLine()) != null) {
                if (!l.trim().isEmpty()) lines.add(l.trim());
            }
        }
        p.waitFor();
        for (String f : lines) {
            churn.put(f, churn.getOrDefault(f, 0) + 1);
        }
        return churn;
    }

    // Small helper classes
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
