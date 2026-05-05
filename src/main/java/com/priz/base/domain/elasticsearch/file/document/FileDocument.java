package com.priz.base.domain.elasticsearch.file.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "files", createIndex = true)
public class FileDocument {

    @Id
    private String id; // This matches the MySQL FileModel ID

    @Field(type = FieldType.Text, analyzer = "standard")
    private String originalName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String fileType; // e.g., "txt", "md"

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;
}
