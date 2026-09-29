package az.abb.embassyflow.order.dao.converter;

import az.abb.embassyflow.order.enums.Period;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PeriodConverter implements AttributeConverter<Period, String> {

    @Override
    public String convertToDatabaseColumn(Period attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public Period convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Period.fromCode(dbData);
    }
}
