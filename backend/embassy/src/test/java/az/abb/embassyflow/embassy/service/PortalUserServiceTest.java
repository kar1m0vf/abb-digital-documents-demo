package az.abb.embassyflow.embassy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.embassy.dao.entity.PortalUser;
import az.abb.embassyflow.embassy.dao.repository.PortalUserRepository;
import az.abb.embassyflow.embassy.dto.response.PortalUserInfo;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PortalUserServiceTest {

    @Mock
    private PortalUserRepository portalUserRepository;

    @InjectMocks
    private PortalUserService portalUserService;

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
    void authenticate_success_returnsInfoWithoutPassword() {
        when(portalUserRepository.findByUsernameAndActiveTrue("admin@italy")).thenReturn(Optional.of(user()));

        PortalUserInfo info = portalUserService.authenticate("admin@italy", "demo1234");

        assertEquals(1L, info.id());
        assertEquals(1L, info.embassyId());
        assertEquals("Aydan Əhadova", info.fullName());
        assertEquals("ADMIN", info.role());
    }

    @Test
    void authenticate_wrongPassword_throwsInvalidCredentials() {
        when(portalUserRepository.findByUsernameAndActiveTrue("admin@italy")).thenReturn(Optional.of(user()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> portalUserService.authenticate("admin@italy", "wrong"));

        assertEquals(ErrorCodes.INVALID_CREDENTIALS, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void authenticate_unknownUser_throwsInvalidCredentials() {
        when(portalUserRepository.findByUsernameAndActiveTrue("admin@italy")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> portalUserService.authenticate("admin@italy", "demo1234"));

        assertEquals(ErrorCodes.INVALID_CREDENTIALS, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void findActiveById_nullId_returnsNull() {
        assertNull(portalUserService.findActiveById(null));
    }

    @Test
    void findActiveById_unknownUser_returnsNull() {
        when(portalUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(portalUserService.findActiveById(99L));
    }

    @Test
    void findActiveById_inactiveUser_returnsNull() {
        PortalUser user = user();
        user.setActive(false);
        when(portalUserRepository.findById(1L)).thenReturn(Optional.of(user));

        assertNull(portalUserService.findActiveById(1L));
    }

    @Test
    void findActiveById_activeUser_returnsInfo() {
        when(portalUserRepository.findById(1L)).thenReturn(Optional.of(user()));

        PortalUserInfo info = portalUserService.findActiveById(1L);

        assertEquals(1L, info.embassyId());
        assertEquals("Aydan Əhadova", info.fullName());
    }
}