package com.skillforge.skillforge;

import com.skillforge.skillforge.repository.OwnershipTransferRepository;
import com.skillforge.skillforge.repository.PortfolioItemRepository;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import com.skillforge.skillforge.repository.SkillRepository;
import com.skillforge.skillforge.repository.StudentProfileRepository;
import com.skillforge.skillforge.repository.StudentSkillRepository;
import com.skillforge.skillforge.repository.TaskRepository;
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

    @MockitoBean
    private OwnershipTransferRepository ownershipTransferRepository;

    @MockitoBean
    private PortfolioItemRepository portfolioItemRepository;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private StudentProfileRepository studentProfileRepository;

    @MockitoBean
    private StudentSkillRepository studentSkillRepository;

    @MockitoBean
    private TaskRepository taskRepository;

    @Test
    void contextLoads() {
    }
}
