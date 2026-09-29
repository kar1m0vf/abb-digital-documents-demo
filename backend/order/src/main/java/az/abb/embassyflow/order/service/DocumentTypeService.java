package az.abb.embassyflow.order.service;

import az.abb.embassyflow.order.dto.response.DocumentTypeResponse;
import az.abb.embassyflow.order.enums.DocumentType;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DocumentTypeService {

    public List<DocumentTypeResponse> list() {
        boolean english = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        return Arrays.stream(DocumentType.values())
                .map(type -> new DocumentTypeResponse(
                        type.name(),
                        english ? type.getNameEn() : type.getNameAz(),
                        english ? type.getDescriptionEn() : type.getDescriptionAz(),
                        type.getPrice(),
                        type.getCurrency()))
                .toList();
    }
}