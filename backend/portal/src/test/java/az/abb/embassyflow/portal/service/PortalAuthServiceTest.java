package az.abb.embassyflow.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.embassy.dao.entity.PortalUser;
import az.abb.embassyflow.embassy.dao.repository.PortalUserRepository;
import az.abb.embassyflow.embassy.service.EmbassyService;
import az.abb.embassyflow.portal.dto.request.PortalLoginRequest;
import az.abb.embassyflow.portal.dto.response.PortalLoginResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PortalAuthServiceTest {

    @Mock
    private PortalUserRepository portalUserRepository;

    @Mock
    private EmbassyService embassyService;

    @InjectMocks
    private PortalAuthService portalAuthService;

    private PortalUser user() {
        PortalUser user = new PortalUser();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setEmbassyId(1L);
        user.setUsername("admin@italy");
        user.setPassword("demo1234");
        user.setFullName("Aydan Əhadova");
        user.setRole("ADMIN");
        user.setActive(true);
        return user;
    }

    @Test
    void login_successReturnsTokenAndEmbassy() {
        when(portalUserRepository.findByUsernameAndActiveTrue("admin@italy")).thenReturn(Optional.of(user()));
        when(embassyService.findName(1L)).thenReturn(Optional.of("İtaliya səfirliyi"));

        PortalLoginResponse response =
                portalAuthService.login(new PortalLoginRequest("admin@italy", "demo1234"));

        assertEquals("demo-portal-token-1", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals("İtaliya səfirliyi", response.embassyName());
        assertEquals("Aydan Əhadova", response.userName());
        assertEquals("ADMIN", response.role());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(portalUserRepository.findByUsernameAndActiveTrue("admin@italy")).thenReturn(Optional.of(user()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> portalAuthService.login(new PortalLoginRequest("admin@italy", "wrong")));

        assertEquals(ErrorCodes.INVALID_CREDENTIALS, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        when(portalUserRepository.findByUsernameAndActiveTrue("admin@italy")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> portalAuthService.login(new PortalLoginRequest("admin@italy", "demo1234")));

        assertEquals(ErrorCodes.INVALID_CREDENTIALS, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void login_missingEmbassyName_isNullSafe() {
        when(portalUserRepository.findByUsernameAndActiveTrue("admin@italy")).thenReturn(Optional.of(user()));
        when(embassyService.findName(1L)).thenReturn(Optional.empty());

        PortalLoginResponse response =
                portalAuthService.login(new PortalLoginRequest("admin@italy", "demo1234"));

        assertNull(response.embassyName());
    }
}