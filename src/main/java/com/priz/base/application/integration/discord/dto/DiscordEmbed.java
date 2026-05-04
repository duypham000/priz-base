package com.priz.base.application.integration.discord.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DiscordEmbed {

    private String title;
    private String description;

    /** RGB color as integer, e.g. 0xFF5733. */
    private Integer color;

    @Builder.Default
    private List<Field> fields = List.of();

    private Media image;
    private Media thumbnail;
    private Footer footer;
    private Author author;

    @Data
    @Builder
    public static class Field {
        private String name;
        private String value;
        @Builder.Default
        private boolean inline = false;
    }

    @Data
    @Builder
    public static class Media {
        private String url;
    }

    @Data
    @Builder
    public static class Footer {
        private String text;
    }

    @Data
    @Builder
    public static class Author {
        private String name;
    }
}
