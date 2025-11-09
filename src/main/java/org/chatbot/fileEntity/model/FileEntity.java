package org.chatbot.fileEntity.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "files")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean active;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String json;

    @Column(nullable = false)
    private Instant createdAt;
}
