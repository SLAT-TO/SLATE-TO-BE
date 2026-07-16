package com.slatto.domain.video.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class YoutubeApiClient {

    private static final String YOUTUBE_API_BASE_URL = "https://www.googleapis.com";
    private static final List<String> THUMBNAIL_PRIORITIES =
            List.of("maxres", "standard", "high", "medium", "default");

    private final RestClient restClient;
    private final String apiKey;

    public YoutubeApiClient(RestClient.Builder restClientBuilder, @Value("${youtube.api.key}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(YOUTUBE_API_BASE_URL).build();
        this.apiKey = apiKey;
    }

    public Optional<YoutubeVideoInfo> getVideo(String youtubeVideoId) {
        YoutubeVideosResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/youtube/v3/videos")
                        .queryParam("part", "snippet,contentDetails,status")
                        .queryParam("id", youtubeVideoId)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(YoutubeVideosResponse.class);

        if (response == null || response.items() == null || response.items().isEmpty()) {
            return Optional.empty();
        }

        YoutubeVideoItem item = response.items().getFirst();
        String thumbnailUrl = selectThumbnailUrl(item.snippet().thumbnails());
        int durationSeconds = Math.toIntExact(Duration.parse(item.contentDetails().duration()).getSeconds());

        return Optional.of(new YoutubeVideoInfo(
                item.snippet().title(),
                thumbnailUrl,
                durationSeconds,
                item.status().embeddable(),
                item.status().privacyStatus()
        ));
    }

    private String selectThumbnailUrl(Map<String, YoutubeThumbnail> thumbnails) {
        if (thumbnails == null) {
            return null;
        }

        return THUMBNAIL_PRIORITIES.stream()
                .map(thumbnails::get)
                .filter(thumbnail -> thumbnail != null && thumbnail.url() != null)
                .map(YoutubeThumbnail::url)
                .findFirst()
                .orElse(null);
    }

    public record YoutubeVideoInfo(
            String title,
            String thumbnailUrl,
            int durationSeconds,
            boolean embeddable,
            String privacyStatus
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YoutubeVideosResponse(List<YoutubeVideoItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YoutubeVideoItem(
            YoutubeSnippet snippet,
            YoutubeContentDetails contentDetails,
            YoutubeStatus status
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YoutubeSnippet(
            String title,
            Map<String, YoutubeThumbnail> thumbnails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YoutubeThumbnail(String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YoutubeContentDetails(String duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YoutubeStatus(
            boolean embeddable,
            String privacyStatus
    ) {
    }
}
