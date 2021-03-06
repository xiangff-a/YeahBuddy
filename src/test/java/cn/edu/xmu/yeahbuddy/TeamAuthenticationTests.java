package cn.edu.xmu.yeahbuddy;

import cn.edu.xmu.yeahbuddy.domain.Administrator;
import cn.edu.xmu.yeahbuddy.domain.Team;
import cn.edu.xmu.yeahbuddy.model.TeamDto;
import cn.edu.xmu.yeahbuddy.service.TeamService;
import cn.edu.xmu.yeahbuddy.service.YbPasswordEncodeService;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RunWith(SpringRunner.class)
@SpringBootTest
@Rollback
@TestExecutionListeners(listeners = {WithSecurityContextTestExecutionListener.class})
public class TeamAuthenticationTests extends AbstractTransactionalJUnit4SpringContextTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Autowired
    private YbPasswordEncodeService ybPasswordEncodeService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeTransaction
    public void setUp() {
        new TransactionTemplate(transactionManager).execute(status -> {
            Administrator ultimate = new Administrator();
            ultimate.setAuthorities(Arrays.asList(Administrator.AdministratorPermission.values()));
            SecurityContextHolder.getContext().setAuthentication(ultimate);
            teamService.registerNewTeam(
                    new TeamDto()
                            .setUsername("someteam")
                            .setPassword("some")
                            .setDisplayName("someteam")
                            .setEmail("c@f.com")
                            .setPhone("18888888888")
                            .setProjectName("buddy"));
            teamService.registerNewTeam(
                    new TeamDto()
                            .setUsername("otherteam")
                            .setPassword("one")
                            .setDisplayName("otherteam")
                            .setEmail("d@f.com")
                            .setPhone("17788888888")
                            .setProjectName("yeah"));
            SecurityContextHolder.getContext().setAuthentication(null);

            return null;
        });
    }

    @AfterTransaction
    public void tearDown() {
        new TransactionTemplate(transactionManager).execute(status -> {
            Administrator ultimate = new Administrator();
            ultimate.setAuthorities(Arrays.asList(Administrator.AdministratorPermission.values()));
            SecurityContextHolder.getContext().setAuthentication(ultimate);
            teamService.deleteTeam(teamService.loadUserByUsername("someteam").getId());
            teamService.deleteTeam(teamService.loadUserByUsername("otherteam").getId());
            SecurityContextHolder.getContext().setAuthentication(null);
            return null;
        });
    }

    @Test
    @WithUserDetails(value = "someteam", userDetailsServiceBeanName = "teamService")
    public void updateTeamTest() {
        Team team = teamService.loadUserByUsername("someteam");
        Assert.assertEquals("buddy", team.getProjectName());
        teamService.updateTeam(team.getId(), new TeamDto().setProjectName("bbudy"));
        Assert.assertEquals("bbudy", team.getProjectName());
        team = teamService.updateTeam(team.getId(), new TeamDto().setUsername("some"));
        Assert.assertEquals("some", team.getUsername());
    }


    @Test
    @WithUserDetails(value = "someteam", userDetailsServiceBeanName = "teamService")
    public void updateTeamPasswordTest() {
        Team team = teamService.loadUserByUsername("someteam");
        Assert.assertTrue(ybPasswordEncodeService.matches("some", team.getPassword()));
        team = teamService.updateTeamPassword(team.getId(), "some", "someteampass");
        Assert.assertTrue(ybPasswordEncodeService.matches("someteampass", team.getPassword()));
        exception.expect(BadCredentialsException.class);
        teamService.updateTeamPassword(team.getId(), "bad", "bad");
    }

    @Test
    @WithUserDetails(value = "otherteam", userDetailsServiceBeanName = "teamService")
    public void updateTeamWithoutAuthTest() {
        Team team = teamService.loadUserByUsername("someteam");
        exception.expect(AccessDeniedException.class);
        teamService.updateTeam(team.getId(), new TeamDto().setProjectName("bbudy"));
    }
}