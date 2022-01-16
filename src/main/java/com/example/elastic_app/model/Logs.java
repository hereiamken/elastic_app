package com.example.elastic_app.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Logs class
 */
@Document(indexName = "access-*", createIndex = false)
@Setting(settingPath = "static/es-settings.json")
@Data
public class Logs {

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Text)
    private String verb;

    @Field(type = FieldType.Object)
    private GeoIp geoip;

    @Field(type = FieldType.Object)
    private UserAgent ua;

    @Field(type = FieldType.Date, name = "@timestamp")
    private LocalDateTime timestamp;

    @Field(type = FieldType.Integer)
    private BigDecimal response;
}
