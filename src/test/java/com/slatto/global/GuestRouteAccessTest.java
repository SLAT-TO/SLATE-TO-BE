package com.slatto.global;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GuestRouteAccessTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("게스트 참고 파일 경로는 인증 없이 통과하고 링크 검증까지 도달한다")
    void 게스트_참고_파일_경로는_인증_없이_통과한다() throws Exception {
        mockMvc.perform(get("/api/v1/share-links/nope/files")
                        .header("X-Guest-Id", 1)
                        .header("X-Guest-Token", "t"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHARELINK404"));

        mockMvc.perform(get("/api/v1/share-links/nope/files/7/download")
                        .header("X-Guest-Id", 1)
                        .header("X-Guest-Token", "t"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHARELINK404"));
    }

    @Test
    @DisplayName("게스트 헤더가 없으면 400 이다 — 기존 video 경로도 같다")
    void 게스트_헤더가_없으면_400_이다() throws Exception {
        mockMvc.perform(get("/api/v1/share-links/nope/files"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));

        mockMvc.perform(get("/api/v1/share-links/nope/files/7/download"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));

        mockMvc.perform(get("/api/v1/share-links/nope/video"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));
    }

    @Test
    @DisplayName("Swagger 문서에 새 경로가 노출된다")
    void 스웨거_문서에_새_경로가_노출된다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/share-links/{shareToken}/files']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/share-links/{shareToken}/files/{referenceFileId}/download']").exists());
    }
}
