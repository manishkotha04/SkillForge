
package com.skillforge.skillforge;

import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.jpa.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
class SkillforgeApplicationTests {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private ProjectMemberRepository projectMemberRepository;

    @MockitoBean
    private ProjectInvitationRepository projectInvitationRepository;

    @Test
    void contextLoads() {
    }
}