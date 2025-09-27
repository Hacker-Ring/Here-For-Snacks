package main.java.com.hfs.stride.analysis;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, Integer> fileTypes = new HashMap<>();

        File[] files = repoDir.listFiles();
        if (files != null) {
            for (File f : files) {
                Result result = analyzeFileRecursively(f, fileTypes);
                totalFiles += result.files;
                totalLines += result.lines;
            }
        }

        metrics.put("totalFiles", totalFiles);
        metrics.put("totalLines", totalLines);
        metrics.put("fileTypes", fileTypes);

        return metrics;
    }

    private Result analyzeFileRecursively(File file, Map<String, Integer> fileTypes) {
        int filesCount = 0;
        int linesCount = 0;

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    Result childResult = analyzeFileRecursively(child, fileTypes);
                    filesCount += childResult.files;
                    linesCount += childResult.lines;
                }
            }
        } else {
            filesCount++;
            String ext = getFileExtension(file.getName());
            fileTypes.put(ext, fileTypes.getOrDefault(ext, 0) + 1);

            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
                while (br.readLine() != null) linesCount++;
            } catch (Exception ignored) {}
        }

        return new Result(filesCount, linesCount);
    }

    private String getFileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0 && idx < fileName.length() - 1) return fileName.substring(idx + 1);
        return "no_extension";
    }

    private static class Result {
        int files;
        int lines;

        Result(int files, int lines) {
            this.files = files;
            this.lines = lines;
        }
    }
}
