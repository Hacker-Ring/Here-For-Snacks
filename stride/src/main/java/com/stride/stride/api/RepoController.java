package com.stride.stride.api;

import com.stride.stride.service.StrideService;

import java.util.List;
import java.util.Map;

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
    public Map<String, Object> analyzeRepo(@RequestParam String path) {
    return strideService.analyzeRepo(path);
}

    @PostMapping("/optimize")
    public List<String> optimizeRepo(@RequestParam String path) {
    return strideService.optimizeRepo(path);
}
    @PostMapping("/analyze-optimize")
    public Map<String, Object> analyzeAndOptimizeRepo(@RequestParam String path) {
    return strideService.analyzeAndOptimize(path);
}

}
