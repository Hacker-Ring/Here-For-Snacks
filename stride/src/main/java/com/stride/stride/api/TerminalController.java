package com.stride.stride.api;

import org.springframework.web.bind.annotation.*;
import java.io.*;

@RestController
@CrossOrigin
@RequestMapping("/repo")
public class TerminalController {

    @PostMapping("/{action}")
    public String runAction(@PathVariable String action, @RequestParam String url, @RequestParam String path) 
            throws IOException, InterruptedException {

        if (url == null || url.isEmpty()) return "No GitHub link provided";
        if (!action.equals("clone") && !action.equals("optimize") && !action.equals("analyze")) {
            return "Invalid action: " + action;
        }

        // Construct curl command dynamically based on action
        String curlCommand = String.format(
            "curl.exe -X POST \"http://localhost:8080/repo/%s?url=%s&path=%s\"",
            action, url, path
        );

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("cmd.exe", "/c", curlCommand);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        // Read output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        output.append("\nProcess exited with code ").append(exitCode);

        return output.toString();
    }
}
