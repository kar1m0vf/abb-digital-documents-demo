package az.abb.embassyflow.portal.service;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.embassy.dao.entity.PortalUser;
import az.abb.embassyflow.embassy.dao.repository.PortalUserRepository;
import az.abb.embassyflow.embassy.service.EmbassyService;
import az.abb.embassyflow.portal.dto.request.PortalLoginRequest;
import az.abb.embassyflow.portal.dto.response.PortalLoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalAuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final PortalUserRepository portalUserRepository;
    private final EmbassyService embassyService;

    public PortalAuthService(PortalUserRepository portalUserRepository, EmbassyService embassyService) {
        this.portalUserRepository = portalUserRepository;
        this.embassyService = embassyService;
    }

    @Transactional(readOnly = true)
    public PortalLoginResponse login(PortalLoginRequest request) {
        PortalUser user = portalUserRepository.findByUsernameAndActiveTrue(request.username())
                .filter(u -> u.getPassword().equals(request.password()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.INVALID_CREDENTIALS, "error.invalid_credentials", HttpStatus.UNAUTHORIZED));

        String embassyName = embassyService.findName(user.getEmbassyId()).orElse(null);
        String token = "demo-portal-token-" + user.getId();
        return new PortalLoginResponse(token, TOKEN_TYPE, embassyName, user.getFullName(), user.getRole());
    }
}