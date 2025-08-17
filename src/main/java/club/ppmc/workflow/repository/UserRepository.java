package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author cc
 * @description User 实体的 JPA Repository 接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
}