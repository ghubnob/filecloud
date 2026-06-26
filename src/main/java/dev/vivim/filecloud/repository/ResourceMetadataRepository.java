package dev.vivim.filecloud.repository;

import dev.vivim.filecloud.model.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceMetadataRepository extends JpaRepository<ResourceEntity, Long> {
    @Query("""
select r from ResourceEntity r where r.userId = :userId
 and lower(r.name) like lower(concat('%', :query, '%'))
""")
    List<ResourceEntity> searchByUserIdAndName(@Param("userId") Integer userId, @Param("query") String query);

    Optional<ResourceEntity> findByUserIdAndPathAndName(Integer userId, String path, String name);

    List<ResourceEntity> findAllByUserIdAndPathOrderByResourceTypeDescNameAsc(Integer userId, String path);

    List<ResourceEntity> findAllByUserIdAndPathStartingWith(Integer userId, String pathPrefix);

    void deleteByUserIdAndPathAndName(Integer userId, String path, String name);
}