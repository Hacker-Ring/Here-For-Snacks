package main.java.com.hfs.stride.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalysisResult {
    private int totalFiles;
    private int javaFiles;
    private int totalLines;
    private double avgLinesPerFile;
    private double commentRatio;
    private double cyclomaticComplexity;
}

