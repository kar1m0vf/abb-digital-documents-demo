package az.abb.embassyflow.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import az.abb.embassyflow.embassy.dto.response.PortalUserInfo;
import az.abb.embassyflow.embassy.service.EmbassyService;
import az.abb.embassyflow.embassy.service.PortalUserService;
import az.abb.embassyflow.portal.dto.request.PortalLoginRequest;
import az.abb.embassyflow.portal.dto.response.PortalLoginResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalAuthServiceTest {

    @Mock
    private PortalUserService portalUserService;

    @Mock
    private EmbassyService embassyService;

    @InjectMocks
    private PortalAuthService portalAuthService;

    private PortalUserInfo user() {
        return new PortalUserInfo(1L, 1L, "Aydan Əhadova", "ADMIN");
    }

    @Test
    void login_successReturnsTokenAndEmbassy() {
        when(portalUserService.authenticate("admin@italy", "demo1234")).thenReturn(user());
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
    void login_missingEmbassyName_isNullSafe() {
        when(portalUserService.authenticate("admin@italy", "demo1234")).thenReturn(user());
        when(embassyService.findName(1L)).thenReturn(Optional.empty());

        PortalLoginResponse response =
                portalAuthService.login(new PortalLoginRequest("admin@italy", "demo1234"));

        assertNull(response.embassyName());
    }
}