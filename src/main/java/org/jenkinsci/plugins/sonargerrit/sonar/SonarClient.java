package org.jenkinsci.plugins.sonargerrit.sonar;

import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.sonargerrit.config.SonarInstallationReader;
import org.jenkinsci.plugins.sonargerrit.inspection.entity.Report;

import hudson.AbortException;
import hudson.model.Run;
import hudson.plugins.sonar.SonarInstallation;

public class SonarClient {
    private final String serverUrl;
    private final Client client;

    public SonarClient(String sonarInstallationName, Run<?, ?> run) throws AbortException {
        Optional<SonarInstallation> sonarInstallationOptional = SonarInstallationReader.getSonarInstallation(sonarInstallationName);

        if (sonarInstallationOptional.isPresent()) {
            SonarInstallation sonarInstallation = sonarInstallationOptional.get();
            this.serverUrl = sonarInstallation.getServerUrl();

            StringCredentials credentials = sonarInstallation.getCredentials(run);

            if (credentials == null) {
                throw new AbortException("Missing Server authentication token for SonarQube Server " + sonarInstallation
                        .getName());
            }
            String token = credentials.getSecret().getPlainText();
            HttpAuthenticationFeature basicAuth = HttpAuthenticationFeature.basic(token, "");

            client = ClientBuilder.newClient();
            client.register(basicAuth);
        } else {
            throw new AbortException("SonarQube '" + sonarInstallationName + "' not found -> Add it in Jenkins system configuration - SonarQube servers");
        }
    }

    public Report fetchIssues(String component, String pullrequestKey) {
        WebTarget target = client.target(serverUrl).path("api").path("issues").path("search")
                .queryParam("componentKeys", component)
                .queryParam("pullRequest", pullrequestKey);

        return target.request(MediaType.APPLICATION_JSON_TYPE).get(Report.class);
    }
}
