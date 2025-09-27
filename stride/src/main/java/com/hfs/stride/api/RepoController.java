package main.java.com.hfs.stride.api;

import com.stride.stride.service.StrideService;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/repo")
public class RepoController {

    private final StrideService strideService;

    public RepoController(StrideService strideService) {
        this.strideService = strideService;
    }

    @PostMapping("/clone")
    public String cloneRepo(@RequestParam String url, @RequestParam String path) {
        return strideService.cloneRepo(url, path);
    }

    @PostMapping("/analyze")
    public AnalysisResult analyzeRepo(@RequestParam String url) throws Exception {} {
        return strideService.analyzeRepo(url);
    }

    @PostMapping("/optimize")
    public OptimizationResult optimizeRepo(@RequestParam String url) throws Exception {
        return strideService.optimizeRepo(url);
    }

    @GetMapping("/result")
    public RepoEntity getRepoResult(@RequestParam String url) {
        return strideService.getRepoResult(url);
    }

}
