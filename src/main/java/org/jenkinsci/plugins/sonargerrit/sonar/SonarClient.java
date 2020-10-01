package org.jenkinsci.plugins.sonargerrit.sonar;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.sonargerrit.inspection.entity.Report;
import org.jenkinsci.plugins.sonargerrit.sonar.dto.ComponentSearchResult;

import hudson.AbortException;
import hudson.plugins.sonar.SonarInstallation;

public class SonarClient {
    private final String serverUrl;

    private final Client client;

    public SonarClient(SonarInstallation sonarInstallation, StringCredentials credentials) throws AbortException {
        this.serverUrl = sonarInstallation.getServerUrl();

        if (credentials == null) {
            throw new AbortException("Missing Server authentication token for SonarQube Server " + sonarInstallation.getName());
        }
        String token = credentials.getSecret().getPlainText();
        HttpAuthenticationFeature basicAuth = HttpAuthenticationFeature.basic(token, "");

        client = ClientBuilder.newClient();
        client.register(basicAuth);
    }

    public Report fetchIssues(String component, String pullrequestKey) {
        WebTarget target = client.target(serverUrl).path("api").path("issues").path("search")
                .queryParam("componentKeys", component)
                .queryParam("pullRequest", pullrequestKey);

        return target.request(MediaType.APPLICATION_JSON_TYPE).get(Report.class);
    }

    /**
     * @see  <a href="https://sonarqube.mamdev.server.lan/web_api/api/components">https://sonarqube.mamdev.server.lan/web_api/api/components</a>
     *
     */
    public ComponentSearchResult fetchComponent(String component) {
        ComponentSearchResult componentSearchResult = null;
        Integer total = null;

        for (int currentPage = 1; total == null || (currentPage - 1) * 500 < total; currentPage++) {
            ComponentSearchResult pageResult = fetchComponent(component, currentPage);
            if (componentSearchResult == null) {
                componentSearchResult = pageResult;
                total = pageResult.getPaging().getTotal();
            } else {
                componentSearchResult.getComponents().addAll(pageResult.getComponents());
            }
        }

        return componentSearchResult;
    }

    private ComponentSearchResult fetchComponent(String component, Integer page) {
        WebTarget target = client.target(serverUrl).path("api").path("components").path("search")
                .queryParam("qualifiers", "TRK") // TRK - Projects
                .queryParam("ps", 500) // page size
                .queryParam("p", page); // 1-based page number

        if (component != null && !component.isEmpty()) {
            target = target.queryParam("q", component); // component key
        }

        return target.request(MediaType.APPLICATION_JSON_TYPE).get(ComponentSearchResult.class);
    }

    public String getServerUrl() {
        return serverUrl;
    }
}
