package ma.tourisme.guide.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import ma.tourisme.guide.config.SecurityConfig;
import ma.tourisme.guide.dto.GuideAdminResponse;
import ma.tourisme.guide.dto.GuideRequest;
import ma.tourisme.guide.dto.GuideResponse;
import ma.tourisme.guide.service.GuideService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

/** Access rules of the Guide API with Keycloak tokens (no database, no Keycloak server). */
@WebMvcTest(GuideController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://test-issuer",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:1/certs"
})
class GuideSecurityTest {

    private static final String GUIDE_SUB = "3f2a1b9c-0d4e-4f5a-8b6c-7d8e9f0a1b2c";
    private static final String BODY = """
        {"nom":"Benali","prenom":"Youssef","ville":"Agadir","langues":["français"],
         "email":"youssef@example.com","telephone":"0600000000","tarifParJour":400}
        """;

    @Autowired MockMvc mvc;
    @MockitoBean GuideService service;

    private static GuideResponse guide(long id) {
        return new GuideResponse(id, "Benali", "Youssef", "Agadir", List.of("français"),
            "youssef@example.com", "0600000000", 400.0, true, null);
    }

    private static RequestPostProcessor as(String role) {
        return jwt().jwt(j -> j.subject(GUIDE_SUB))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request) {
        return request.contentType(MediaType.APPLICATION_JSON).content(BODY);
    }

    @Test
    void publicReadsNeedNoTokenAndKeepTheCircuitContract() throws Exception {
        when(service.findAll(null, null)).thenReturn(List.of(guide(1)));
        when(service.findById(1L)).thenReturn(guide(1));

        mvc.perform(get("/api/guides")).andExpect(status().isOk());
        mvc.perform(get("/api/guides/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.disponible").value(true))
            .andExpect(jsonPath("$.ownerSubject").doesNotExist());
    }

    @Test
    void onlyAdminCanCreateUpdateOrArchive() throws Exception {
        when(service.create(any())).thenReturn(guide(5));

        mvc.perform(json(post("/api/guides"))).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/guides")).with(as("USER"))).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/guides")).with(as("GUIDE"))).andExpect(status().isForbidden());
        mvc.perform(json(put("/api/guides/1")).with(as("GUIDE"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/guides/1").with(as("GUIDE"))).andExpect(status().isForbidden());
        verify(service, never()).update(any(), any());
        verify(service, never()).desactiver(any());

        mvc.perform(json(post("/api/guides")).with(as("ADMIN"))).andExpect(status().isCreated());
        mvc.perform(delete("/api/guides/1").with(as("ADMIN"))).andExpect(status().isNoContent());
    }

    @Test
    void meUsesTheTokenSubjectOnly() throws Exception {
        when(service.findByOwner(GUIDE_SUB)).thenReturn(guide(7));

        mvc.perform(get("/api/guides/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/guides/me").with(as("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/guides/me").with(as("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/guides/me").with(as("GUIDE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void meReturns404WhenNoProfileIsLinked() throws Exception {
        when(service.findByOwner(GUIDE_SUB)).thenThrow(
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun profil guide lié à ce compte"));

        
        mvc.perform(get("/api/guides/me").with(as("GUIDE")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Aucun profil guide lié à ce compte"));
    }

    @Test
    void guideUpdatesOnlyTheirOwnProfile() throws Exception {
        when(service.updateMine(eq(GUIDE_SUB), any(GuideRequest.class))).thenReturn(guide(7));

        mvc.perform(json(put("/api/guides/me")).with(as("GUIDE"))).andExpect(status().isOk());
        verify(service).updateMine(eq(GUIDE_SUB), any(GuideRequest.class));
        verify(service, never()).update(any(), any());
    }

    @Test
    void adminListIsAdminOnly() throws Exception {
        when(service.findAllForAdmin()).thenReturn(List.of(new GuideAdminResponse(
            1L, "Benali", null, "Agadir", List.of(), null, null, 400.0, false, false, GUIDE_SUB, null)));

        mvc.perform(get("/api/guides/admin").with(as("GUIDE"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/guides/admin").with(as("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ownerSubject").value(GUIDE_SUB))
            .andExpect(jsonPath("$[0].actif").value(false));
    }

    @Test
    void realmRolesAreMappedAndUnknownRolesIgnored() throws Exception {
        when(service.findByOwner(GUIDE_SUB)).thenReturn(guide(7));
        var realmGuide = jwt()
            .jwt(j -> j.subject(GUIDE_SUB).claim("realm_access", Map.of("roles", List.of("GUIDE", "offline_access"))))
            .authorities(SecurityConfig::realmAuthorities);
        mvc.perform(get("/api/guides/me").with(realmGuide)).andExpect(status().isOk());

        var unknownOnly = jwt()
            .jwt(j -> j.subject(GUIDE_SUB).claim("realm_access", Map.of("roles", List.of("offline_access"))))
            .authorities(SecurityConfig::realmAuthorities);
        mvc.perform(get("/api/guides/me").with(unknownOnly)).andExpect(status().isForbidden());
    }
}
