package com.stride.stride.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class RepoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;
    private int totalFiles;
    private int javaFiles;
    private int totalLines;

    @ElementCollection
    private List<String> suggestions;
}
