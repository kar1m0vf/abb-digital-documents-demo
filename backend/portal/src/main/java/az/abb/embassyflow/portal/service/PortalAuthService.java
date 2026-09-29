package az.abb.embassyflow.portal.service;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.embassy.dto.response.PortalUserInfo;
import az.abb.embassyflow.embassy.service.EmbassyService;
import az.abb.embassyflow.embassy.service.PortalUserService;
import az.abb.embassyflow.portal.dto.request.PortalLoginRequest;
import az.abb.embassyflow.portal.dto.response.PortalLoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalAuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final PortalUserService portalUserService;
    private final EmbassyService embassyService;

    public PortalAuthService(PortalUserService portalUserService, EmbassyService embassyService) {
        this.portalUserService = portalUserService;
        this.embassyService = embassyService;
    }

    @Transactional(readOnly = true)
    public PortalLoginResponse login(PortalLoginRequest request) {
        PortalUserInfo user = portalUserService.authenticate(request.username(), request.password());

        String embassyName = user.embassyId() == null
                ? null
                : embassyService.findName(user.embassyId()).orElse(null);
        String token = "demo-portal-token-" + user.id();
        return new PortalLoginResponse(token, TOKEN_TYPE, embassyName, user.fullName(), user.role());
    }
}