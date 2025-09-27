package main.java.com.hfs.stride.Repo;

import org.eclipse.jgit.api.Git;
import java.io.File;

public class RepoConnector {

    public void cloneorUpdateRepository(String repoUrl, String localPath) throws Exception {
        File repoDir = new File(localPath);
        if (repoDir.exists()) {
            System.out.println("Repository already exists at " + localPath + ". Pulling latest changes.");
            try (Git git = Git.open(repoDir)) {
                git.pull().call();
                System.out.println("Repository updated successfully.");
            } catch (Exception e) {
                System.err.println("Error updating repository: " + e.getMessage());
                throw e;
            }
            return;
        } else {
                System.out.println("Cloning repository from " + repoUrl + " to " + localPath);
                try (Git git = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(new File(localPath))
                        .call()) {
                    System.out.println("Repository cloned successfully.");
                } catch (Exception e) {
                    System.err.println("Error cloning repository: " + e.getMessage());
                    throw e;
                }
        }
}
}

