package com.stride.stride.UTIL;

import org.eclipse.jgit.api.Git;
import java.io.File;

public class GITUTILS {

    public static String cloneRepo(String repoUrl, String localDir) throws Exception {
        File repoDir = new File(localDir);
        if (repoDir.exists()) {
            throw new RuntimeException("Directory already exists: " + localDir);
        }
        Git.cloneRepository()
           .setURI(repoUrl)
           .setDirectory(repoDir)
           .call()
           .close();
        return repoDir.getAbsolutePath();
    }
}