package com.qar.securitysystem;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "app.security.cookieName=QAR_SESSION",
                "app.security.cookieSecure=false",
                "app.security.cookieSameSite=Strict",
                "app.security.sessionTtlMinutes=60",
                "app.admin.username=admin",
                "app.admin.password=ChangeMe-Admin-Password-Now-2026",
                "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        }
)
public class AuthAndFileFlowTests {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    void register_login_upload_download_admin_export() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"emailOrUsername\":\"admin\",\"password\":\"x\",\"passwordConfirm\":\"x\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"emailOrUsername\":\"user1\",\"password\":\"Passw0rd!\",\"passwordConfirm\":\"Passw0rd!\"}"))
                .andExpect(status().isOk());

        String setCookie = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"emailOrUsername\":\"user1\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).contains("QAR_SESSION=");
        String token = setCookie.split("QAR_SESSION=")[1].split(";", 2)[0];
        Cookie sessionCookie = new Cookie("QAR_SESSION", token);

        byte[] payload = "hello-qar".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", payload);

        String fileJson = mvc.perform(multipart("/api/files")
                        .file(file)
                        .param("policy", "role:user")
                        .cookie(sessionCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(fileJson).contains("\"id\"");
        String fileId = fileJson.split("\"id\":\"")[1].split("\"", 2)[0];

        byte[] downloaded = mvc.perform(get("/api/files/" + fileId + "/download")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(downloaded).isEqualTo(payload);

        mvc.perform(post("/api/feedback")
                        .cookie(sessionCookie)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"type\":\"bug\",\"subject\":\"下载校验\",\"message\":\"下载正常，但我想确认一下流程\",\"relatedFileId\":\"" + fileId + "\"}"))
                .andExpect(status().isOk());

        String adminSetCookie = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"emailOrUsername\":\"admin\",\"password\":\"ChangeMe-Admin-Password-Now-2026\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);
        assertThat(adminSetCookie).contains("QAR_SESSION=");
        String adminToken = adminSetCookie.split("QAR_SESSION=")[1].split(";", 2)[0];
        Cookie adminCookie = new Cookie("QAR_SESSION", adminToken);

        String feedbackList = mvc.perform(get("/api/admin/feedback").cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(feedbackList).contains("下载校验");
        String feedbackId = feedbackList.split("\"id\":\"")[1].split("\"", 2)[0];

        mvc.perform(patch("/api/admin/feedback/" + feedbackId)
                        .cookie(adminCookie)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"status\":\"RESOLVED\",\"adminReply\":\"收到，流程正常\"}"))
                .andExpect(status().isOk());

        byte[] zipBytes = mvc.perform(get("/api/admin/files/export").cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        boolean found = false;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith("_a.txt")) {
                    byte[] buf = zis.readAllBytes();
                    assertThat(buf).isEqualTo(payload);
                    found = true;
                    break;
                }
            }
        }
        assertThat(found).isTrue();
    }
}
