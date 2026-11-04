package com.skillforge.skillforge;

import com.skillforge.skillforge.repository.AchievementRepository;
import com.skillforge.skillforge.repository.LeaveRequestRepository;
import com.skillforge.skillforge.repository.OwnershipTransferRepository;
import com.skillforge.skillforge.repository.PortfolioItemRepository;
import com.skillforge.skillforge.repository.ProjectActivityRepository;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import com.skillforge.skillforge.repository.ProjectJoinRequestRepository;
import com.skillforge.skillforge.repository.ProjectMemberRepository;
import com.skillforge.skillforge.repository.ProjectRepository;
import com.skillforge.skillforge.repository.SkillRepository;
import com.skillforge.skillforge.repository.StudentProfileRepository;
import com.skillforge.skillforge.repository.StudentSkillRepository;
import com.skillforge.skillforge.repository.TaskMessageRepository;
import com.skillforge.skillforge.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
        "org.springframework.boot.jpa.autoconfigure.HibernateJpaAutoConfiguration"
})
class SkillforgeApplicationTests {
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AchievementRepository achievementRepository;
    @MockitoBean private ProjectRepository projectRepository;
    @MockitoBean private ProjectMemberRepository projectMemberRepository;
    @MockitoBean private ProjectInvitationRepository projectInvitationRepository;
    @MockitoBean private ProjectJoinRequestRepository projectJoinRequestRepository;
    @MockitoBean private OwnershipTransferRepository ownershipTransferRepository;
    @MockitoBean private LeaveRequestRepository leaveRequestRepository;
    @MockitoBean private PortfolioItemRepository portfolioItemRepository;
    @MockitoBean private SkillRepository skillRepository;
    @MockitoBean private StudentProfileRepository studentProfileRepository;
    @MockitoBean private StudentSkillRepository studentSkillRepository;
    @MockitoBean private TaskRepository taskRepository;
    @MockitoBean private ProjectActivityRepository projectActivityRepository;
    @MockitoBean private TaskMessageRepository taskMessageRepository;

    @Test void contextLoads() { }
}
