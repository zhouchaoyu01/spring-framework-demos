package com.coding.cz.recon.util;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */
import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import com.coding.cz.recon.entity.ReconciliationRuleEntity.CompareField;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;


@Converter
public class CompareFieldListConverter implements AttributeConverter<List<CompareField>, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<CompareField> attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert CompareField list to JSON", e);
        }
    }

    @Override
    public List<CompareField> convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<CompareField>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to CompareField list", e);
        }
    }
}