package com.example.elastic_app.model;

import lombok.Data;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Data
public class GeoIp {

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String city_name;

    private String continent_code;

    private String country_code2;

    private String country_code3;

    private String country_name;

    private String region_code;

    private String ip;
}
