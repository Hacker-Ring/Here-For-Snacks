package main.java.com.hfs.stride.service;

import com.stride.stride.repo.RepoConnector;
import com.stride.stride.analysis.Analyzer;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

@Service
public class StrideService {

    private final RepoConnector repoConnector;
    private final Analyzer analyzer;
    private final OptimizerEngine optimizer;
    private final RepoResultRepository repoResultRepository;

    public StrideService() {
        this.repoConnector = new RepoConnector();
        this.analyzer = new Analyzer();
        this.optimizer = new OptimizerEngine();
        this.repoResultRepository = new RepoResultRepository();
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

    //public Map<String, Object> analyzeRepo(String localDir) {
    //    return analyzer.analyzeRepo(localDir);
    //}

    //public List<String> optimizeRepo(String localDir) {
    //    Map<String, Object> metrics = analyzeRepo(localDir);
    //    return optimizer.generateSuggestions(metrics);
    //}

    public AnalysisResult analyzeRepo(String url) throws Exception {
        File repoDir = repoConnector.cloneRepository(url, "./repos");
        return analyzer.analyzeRepo(repoDir);
    }
    
    public OptimizationResult optimizeRepo(String url) throws Exception {
        File repoDir = repoConnector.cloneRepository(url, "./repos");
        AnalysisResult analysis = analyzer.analyzeRepo(repoDir);
        return optimizer.optimize(analysis);
    }
    public RepoEntity analyzeAndSave(String url) throws Exception {
        File repoDir = repoConnector.cloneRepository(url, "./repos");
        AnalysisResult analysis = analyzer.analyzeRepo(repoDir);
        OptimizationResult optimization = optimizer.optimize(analysis);

        RepoEntity entity = new RepoEntity();
        entity.setUrl(url);
        entity.setTotalFiles(analysis.getTotalFiles());
        entity.setJavaFiles(analysis.getJavaFiles());
        entity.setTotalLines(analysis.getTotalLines());
        entity.setSuggestions(optimization.getSuggestions());

        return repoResultRepository.save(entity);
    }

    public RepoEntity getRepoResult(String url) {
        return repoResultRepository.findByUrl(url);
    }
}
