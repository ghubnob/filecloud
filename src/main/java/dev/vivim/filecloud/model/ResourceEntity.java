package dev.vivim.filecloud.model;

import dev.vivim.filecloud.dto.FileType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.Hibernate;

import java.util.Objects;

@Entity
@Table(name = "resources", uniqueConstraints = {
        @UniqueConstraint(name = "uk_resource_unique", columnNames = {"user_id", "path", "name"})})
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Integer userId;

    @Column(nullable = false)
    String path;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    Long size;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    FileType resourceType;

    public static ResourceEntity createOf(Integer userId, String path, String name, Long size, FileType fileType) {
        return new ResourceEntity(null, userId, path, name, size, fileType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceEntity r)) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(r)) return false;
        return id != null && Objects.equals(id, r.id);
    }
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
