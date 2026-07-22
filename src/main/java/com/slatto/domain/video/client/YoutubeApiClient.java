package com.slatto.domain.video.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    @Autowired
    public YoutubeApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${youtube.api.key}") String apiKey,
            @Value("${youtube.api.connect-timeout}") Duration connectTimeout,
            @Value("${youtube.api.read-timeout}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = restClientBuilder
                .baseUrl(YOUTUBE_API_BASE_URL)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
    }

    YoutubeApiClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public Optional<YoutubeVideoInfo> getVideo(String youtubeVideoId) {
        YoutubeVideosResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/youtube/v3/videos")
                            .queryParam("part", "snippet,contentDetails,status")
                            .queryParam("id", youtubeVideoId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(YoutubeVideosResponse.class);
        } catch (RestClientException exception) {
            throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }

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
    record YoutubeVideosResponse(List<YoutubeVideoItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeVideoItem(
            YoutubeSnippet snippet,
            YoutubeContentDetails contentDetails,
            YoutubeStatus status
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeSnippet(
            String title,
            Map<String, YoutubeThumbnail> thumbnails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeThumbnail(String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeContentDetails(String duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeStatus(
            boolean embeddable,
            String privacyStatus
    ) {
    }
}
