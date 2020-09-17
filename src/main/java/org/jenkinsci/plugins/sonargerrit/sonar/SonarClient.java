package org.jenkinsci.plugins.sonargerrit.sonar;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.jenkinsci.plugins.sonargerrit.inspection.entity.Report;

public class SonarClient {
    private final String serverUrl;
    private final Client client;

    public SonarClient(String serverUrl, String token) {
        this.serverUrl = serverUrl;
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
}
