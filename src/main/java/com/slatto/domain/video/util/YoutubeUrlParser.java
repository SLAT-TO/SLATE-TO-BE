package com.slatto.domain.video.util;

import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@Component
public class YoutubeUrlParser {

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{6,100}");

    public String extractVideoId(String youtubeUrl) {
        try {
            URI uri = new URI(youtubeUrl.trim());
            validateScheme(uri);

            String host = uri.getHost();
            if (host == null) {
                throwBadRequest();
            }

            String normalizedHost = host.toLowerCase();
            if (normalizedHost.equals("youtu.be")) {
                return requireVideoId(pathSegment(uri, 0));
            }
            if (!isYoutubeHost(normalizedHost)) {
                throwBadRequest();
            }

            String contentType = pathSegment(uri, 0);
            if ("shorts".equals(contentType) || "embed".equals(contentType)) {
                return requireVideoId(pathSegment(uri, 1));
            }

            if ("/watch".equals(uri.getPath()) && uri.getRawQuery() != null) {
                for (String parameter : uri.getRawQuery().split("&")) {
                    String[] keyValue = parameter.split("=", 2);
                    if (keyValue.length == 2 && "v".equals(keyValue[0])) {
                        return requireVideoId(keyValue[1]);
                    }
                }
            }
        } catch (URISyntaxException | IndexOutOfBoundsException exception) {
            throw new BaseException(CommonErrorCode.BAD_REQUEST);
        }

        throw new BaseException(CommonErrorCode.BAD_REQUEST);
    }

    private void validateScheme(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throwBadRequest();
        }
    }

    private boolean isYoutubeHost(String host) {
        return host.equals("youtube.com")
                || host.equals("www.youtube.com")
                || host.equals("m.youtube.com");
    }

    private String pathSegment(URI uri, int index) {
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            throwBadRequest();
        }

        String[] segments = path.substring(1).split("/");
        if (index >= segments.length) {
            throwBadRequest();
        }
        return segments[index];
    }

    private String requireVideoId(String videoId) {
        if (videoId == null || !VIDEO_ID_PATTERN.matcher(videoId).matches()) {
            throwBadRequest();
        }
        return videoId;
    }

    private void throwBadRequest() {
        throw new BaseException(CommonErrorCode.BAD_REQUEST);
    }
}
