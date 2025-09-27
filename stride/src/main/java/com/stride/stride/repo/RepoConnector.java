package com.stride.stride.repo;

import org.eclipse.jgit.api.Git;

import java.io.File;

public class RepoConnector {

    /**
     * Clone a repository from GitHub into a local directory.
     * @param repoUrl URL of the GitHub repository
     * @param localPath Path to clone the repository into
     * @throws Exception if cloning fails
     */
    public void cloneRepository(String repoUrl, String localPath) throws Exception {
        System.out.println("Cloning from " + repoUrl + " to " + localPath);

        try (Git result = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localPath))
                .call()) {

            System.out.println("Repository cloned: " + result.getRepository().getDirectory());
        }
    }
}
