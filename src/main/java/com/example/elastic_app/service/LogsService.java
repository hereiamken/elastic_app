package com.example.elastic_app.service;

import com.example.elastic_app.model.Logs;
import com.example.elastic_app.repository.LogsRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LogsService {

    private final LogsRepository logsRepository;

    private final RestHighLevelClient client;

    /**
     * get log with verb GET
     * @param verb
     * @param pageable
     * @return
     */
    public Page<Logs> getLogs(String verb, Pageable pageable){
        return logsRepository.findAllByVerb(verb, pageable);
    }

    public Long countByVerb(String verb){
        return logsRepository.countAllByVerb(verb);
    }

    public ByteArrayInputStream exportBrowser(String filterField, String INDEX, String start, String end, String... fields) throws IOException {
        List<StringBuilder> builderList = aggregation(filterField, INDEX, start, end, fields);
        var bis =  writeTextToPdf("Stats_of_browser", builderList, getTitleName(fields[0]));
        return bis;
    }

    private String getTitleName(String field){
        String value = "";
        if(field.contains("ua")){
            value = "Distribution by type browsers of requests";
        }else if(field.contains("response")){
            value = "Distribution by type responses of requests";
        }else if(field.contains("geoip")){
            value = "Distribution by geographical area of requests";
        }
        return value;
    }

    public ByteArrayInputStream writeTextToPdf(String fileName, List<StringBuilder> data, String title) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

            document.open();

            Font font = FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK);
            Paragraph para = new Paragraph( title, font);
            para.setAlignment(Element.ALIGN_CENTER);
            document.add(para);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(3);
            // Add PDF Table Header ->
            Stream.of("ID", "Key", "Value")
                    .forEach(headerTitle -> {
                        PdfPCell header = new PdfPCell();
                        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
                        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        header.setHorizontalAlignment(Element.ALIGN_CENTER);
                        header.setBorderWidth(2);
                        header.setPhrase(new Phrase(headerTitle, headFont));
                        table.addCell(header);
                    });

            // add item to List
            Integer index = 1;
            for(StringBuilder st : data){

                PdfPCell idCell = new PdfPCell(new Phrase(String.valueOf(index)));
                idCell.setPaddingLeft(4);
                idCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(idCell);

                PdfPCell firstNameCell = new PdfPCell(new Phrase(getValue(st, 0)));
                firstNameCell.setPaddingLeft(4);
                firstNameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                firstNameCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(firstNameCell);

                PdfPCell lastNameCell = new PdfPCell(new Phrase(getValue(st, 1)));
                lastNameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                lastNameCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                lastNameCell.setPaddingRight(4);
                table.addCell(lastNameCell);
                index ++;
            }

            document.add(table);
            document.close();
            writer.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private String getValue(StringBuilder st, int index){
        String value = "";
        if(Objects.nonNull(st)){
            if(st.toString().contains(":")){
                String[] arr = st.toString().split(":");
                value = arr[index];
            }
        }
        return value;
    }

    public List<StringBuilder> aggregation(String filterField, String INDEX, String start, String end, String... fields) throws IOException {
        List<StringBuilder> results = new LinkedList<>();
        SearchRequest searchRequest = new SearchRequest(INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if(!StringUtils.isEmpty(filterField)) {
            QueryBuilder queryBuilder = QueryBuilders.rangeQuery(filterField).gte(start).lte(end);
            searchSourceBuilder.query(queryBuilder);
        }

        AggregationBuilder aggregationBuilder = buildAggregation(fields, 0);

        if(aggregationBuilder == null) {
            return results;
        }
        searchSourceBuilder.aggregation(aggregationBuilder);

        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Terms terms = searchResponse.getAggregations().get(fields[0]);
        processBuckets(terms, new Stack<>(), results);
        return results;
    }

    private AggregationBuilder buildAggregation(String[] fields, int i) {
        if(i == fields.length - 1)
            return aggregationBuilder(fields[i]);

        return aggregationBuilder(fields[i]).subAggregation(buildAggregation(fields, i + 1));
    }

    private AggregationBuilder aggregationBuilder(String term) {
        return AggregationBuilders
                .terms(term)
                .field(term)
                .size(10000)
                .order(BucketOrder.count(false));
    }

    private void processBuckets(Terms terms, Stack<String> stack, List<StringBuilder> results) {
        if(terms != null) {
            for (Terms.Bucket bucket : terms.getBuckets()) {
                stack.push(bucket.getKeyAsString() + ":" + bucket.getDocCount());
                if(bucket.getAggregations().asList().isEmpty()) {
                    StringBuilder result = new StringBuilder();
                    for (String s : stack) {
                        result.append(s);
                    }
                    results.add(result);
                }

                for (Aggregation aggregation : bucket.getAggregations().asList()) {
                    processBuckets((Terms) aggregation, stack, results);
                }
                stack.pop();
            }
        }
    }
}
