package com.example.elastic_app.repository;

import com.example.elastic_app.model.Logs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogsRepository extends ElasticsearchRepository<Logs, String> {

    Page<Logs> findAllByVerb(String verb, Pageable pageable);

    Long countAllByVerb(String verb);
}
