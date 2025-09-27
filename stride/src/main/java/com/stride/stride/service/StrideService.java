package com.stride.stride.service;

import com.stride.stride.repo.RepoConnector;
import com.stride.stride.analysis.Analyzer;
//import com.stride.stride.model.AnalysisResult;
import com.stride.stride.optimizer.OptimizerEngine;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;

@Service
public class StrideService {

    private final RepoConnector repoConnector;
    private final Analyzer analyzer;
    private final OptimizerEngine optimizer;

    public StrideService() {
        this.repoConnector = new RepoConnector();
        this.analyzer = new Analyzer();
        this.optimizer = new OptimizerEngine();
    }

    public String cloneRepo(String url, String localDir) {
        try {
            repoConnector.cloneRepository(url, localDir);
            return "Repository cloned successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error cloning repo: " + e.getMessage();
        }
    }

    public Map<String, Object> analyzeRepo(String localDir) {
        return analyzer.analyzeRepo(localDir);
    }

    public List<String> optimizeRepo(String localDir) {
        Map<String, Object> metrics = analyzeRepo(localDir);
        return optimizer.generateSuggestions(metrics);
    }

    public Map<String, Object> analyzeAndOptimize(String path) {
        Map<String, Object> metrics = analyzer.analyzeRepo(path);
        List<String> suggestions = optimizer.generateSuggestions(metrics);
        metrics.put("optimizerSuggestions", suggestions);
        return metrics;
    }
    
}
