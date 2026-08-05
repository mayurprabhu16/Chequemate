package com.chequemate.repository;

import com.chequemate.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.members m WHERE m.id = :userId")
    List<Group> findByUserId(@Param("userId") Long userId);

    // Added missing native query method
    @Query(value = "SELECT g.* FROM groups g JOIN group_members gm ON g.id = gm.group_id WHERE gm.user_id = :userId", nativeQuery = true)
    List<Group> findGroupsByUserIdNative(@Param("userId") Long userId);
}