package com.slatto.domain.video.util;

import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YoutubeUrlParser {

    private static final Pattern YOUTUBE_PATH_PATTERN = Pattern.compile("^/(?:shorts|embed)/([^/?#]+)");
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
                return requireVideoId(firstPathSegment(uri));
            }
            if (!isYoutubeHost(normalizedHost)) {
                throwBadRequest();
            }

            Matcher pathMatcher = YOUTUBE_PATH_PATTERN.matcher(uri.getPath());
            if (pathMatcher.find()) {
                return requireVideoId(pathMatcher.group(1));
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

    private String firstPathSegment(URI uri) {
        return uri.getPath().substring(1).split("/", 2)[0];
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
