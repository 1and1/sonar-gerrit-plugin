package org.jenkinsci.plugins.sonargerrit.sonar;

import javax.json.bind.JsonbBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.jenkinsci.plugins.sonargerrit.inspection.entity.Report;

import groovy.json.JsonBuilder;

public class SonarClient {
    private final WebTarget target;

    public SonarClient() {
        HttpAuthenticationFeature basicAuth = HttpAuthenticationFeature
                .basic("b85cafcf1b795cd898c8dcb6c7c2cdf18d7398ee", "");
        Client client = ClientBuilder.newClient();
        client.register(basicAuth);
        target = client.target("https://sonarqube-test.mamdev.server.lan").path("api").path("issues").path("search")
                .queryParam("componentKeys" , "com.unitedinternet.id.testrp:id-testrp")
                .queryParam("pullRequest" , "11220");
    }

    void fetch() {
//        Report report = target.request(MediaType.APPLICATION_JSON_TYPE).get(Report.class);
        String report = target.request(MediaType.APPLICATION_JSON_TYPE).get(String.class);

        System.out.println(report);
    }

    public static void main(String[] args) {
        new SonarClient().fetch();

//        Report report = new Report();
//        report.setVersion("v1");
//        System.out.println(JsonbBuilder.create().toJson(report));
    }
}
