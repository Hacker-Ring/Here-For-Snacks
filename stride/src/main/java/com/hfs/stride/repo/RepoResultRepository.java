package main.java.com.hfs.stride.repo;

import com.stride.stride.model.RepoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoResultRepository extends JpaRepository<RepoEntity, Long> {
    RepoEntity findbyUrl(String url);
}
