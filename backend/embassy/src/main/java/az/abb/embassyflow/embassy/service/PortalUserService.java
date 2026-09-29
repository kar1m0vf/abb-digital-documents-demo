package az.abb.embassyflow.embassy.service;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.embassy.dao.entity.PortalUser;
import az.abb.embassyflow.embassy.dao.repository.PortalUserRepository;
import az.abb.embassyflow.embassy.dto.response.PortalUserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalUserService {

    private final PortalUserRepository portalUserRepository;

    public PortalUserService(PortalUserRepository portalUserRepository) {
        this.portalUserRepository = portalUserRepository;
    }

    @Transactional(readOnly = true)
    public PortalUserInfo authenticate(String username, String password) {
        return portalUserRepository.findByUsernameAndActiveTrue(username)
                .filter(u -> u.getPassword().equals(password))
                .map(PortalUserService::toInfo)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.INVALID_CREDENTIALS, "error.invalid_credentials", HttpStatus.UNAUTHORIZED));
    }

    @Transactional(readOnly = true)
    public PortalUserInfo findActiveById(Long id) {
        if (id == null) {
            return null;
        }
        return portalUserRepository.findById(id)
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .map(PortalUserService::toInfo)
                .orElse(null);
    }

    private static PortalUserInfo toInfo(PortalUser user) {
        return new PortalUserInfo(user.getId(), user.getEmbassyId(), user.getFullName(), user.getRole());
    }
}