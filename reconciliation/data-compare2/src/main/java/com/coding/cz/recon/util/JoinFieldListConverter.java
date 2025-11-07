package com.coding.cz.recon.util;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import com.coding.cz.recon.entity.ReconciliationRuleEntity.JoinField;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;


@Converter
public class JoinFieldListConverter implements AttributeConverter<List<JoinField>, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<JoinField> attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JoinField list to JSON", e);
        }
    }

    @Override
    public List<JoinField> convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<JoinField>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to CompareField list", e);
        }
    }
}