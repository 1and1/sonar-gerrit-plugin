package org.jenkinsci.plugins.sonargerrit.sonar;

import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.sonargerrit.inspection.entity.Report;

import hudson.AbortException;
import hudson.model.Run;
import hudson.plugins.sonar.SonarGlobalConfiguration;
import hudson.plugins.sonar.SonarInstallation;
import jenkins.model.GlobalConfiguration;

public class SonarClient {
    private String serverUrl;
    private Client client;

    public SonarClient(String sonarInstallationName, Run<?, ?> run) throws AbortException {
        SonarGlobalConfiguration sonarGlobalConfiguration = GlobalConfiguration.all().get(SonarGlobalConfiguration.class);

        if (sonarGlobalConfiguration == null) {
            throw new AbortException("Missing SonarGlobalConfiguration -> Install SonarQube Scanner for Jenkins: https://plugins.jenkins.io/sonar/");
        }

        Optional<SonarInstallation> sonarInstallationOptional = Arrays.stream(sonarGlobalConfiguration.getInstallations())
                .filter(installation -> installation.getName().equals(sonarInstallationName)).findFirst();

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
            throw new AbortException("No SonarQube server found -> Add one in Jenkins system configuration - SonarQube servers");
        }
    }

    public Report fetchIssues(String component, String pullrequestKey) {
        WebTarget target = client.target(serverUrl).path("api").path("issues").path("search")
                .queryParam("componentKeys", component)
                .queryParam("pullRequest", pullrequestKey);

        return target.request(MediaType.APPLICATION_JSON_TYPE).get(Report.class);
    }
}
