package org.jenkinsci.plugins.sonargerrit.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.sonargerrit.SonarToGerritPublisher;
import org.jenkinsci.plugins.sonargerrit.sonar.SonarClient;
import org.jenkinsci.plugins.sonargerrit.sonar.SonarUtil;
import org.jenkinsci.plugins.sonargerrit.sonar.dto.Component;
import org.jenkinsci.plugins.sonargerrit.sonar.dto.ComponentSearchResult;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.base.MoreObjects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.plugins.sonar.SonarGlobalConfiguration;
import hudson.plugins.sonar.SonarInstallation;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;

/**
 * Project: Sonar-Gerrit Plugin
 * Author:  Tatiana Didik
 * Created: 07.12.2017 13:45
 * $Id$
 */
public class InspectionConfig extends AbstractDescribableImpl<InspectionConfig> {
    @Nonnull
    private String serverURL = DescriptorImpl.SONAR_URL;

    private String pullrequestKey;
    private String component;

    private SubJobConfig baseConfig;

    @Nonnull
    private Collection<SubJobConfig> subJobConfigs;

    private String type;

    private String sonarInstallationName;

    @DataBoundConstructor
    public InspectionConfig() {
        this(DescriptorImpl.SONAR_URL, null, null, DescriptorImpl.BASE_TYPE); // set default values
    }

    @SuppressFBWarnings(value="NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR") // subJobConfigs is initialized in setter
    private InspectionConfig(@Nonnull String serverURL, SubJobConfig baseConfig, List<SubJobConfig> subJobConfigs, String type) {
        setServerURL(serverURL);
        setBaseConfig(baseConfig);
        setSubJobConfigs(subJobConfigs);
        setType(type);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return new DescriptorImpl();
    }

    @Nonnull
    public String getServerURL() {
        return serverURL;
    }

    @DataBoundSetter
    public void setServerURL(@Nonnull String serverURL) {
        this.serverURL = MoreObjects.firstNonNull(Util.fixEmptyAndTrim(serverURL), DescriptorImpl.SONAR_URL);
    }

    public SubJobConfig getBaseConfig() {
        return baseConfig;
    }

    @DataBoundSetter
    public void setBaseConfig(SubJobConfig baseConfig) {
        this.baseConfig = MoreObjects.firstNonNull(baseConfig, new SubJobConfig());
    }

    public Collection<SubJobConfig> getSubJobConfigs() {
        return subJobConfigs;
    }

    public Collection<SubJobConfig> getAllSubJobConfigs() {
        return isMultiConfigMode() ? subJobConfigs : Collections.singletonList(baseConfig);
    }

    public boolean isType(String type) {
        return this.type.equalsIgnoreCase(type);
    }

    @DataBoundSetter
    public void setType(String type) {
        if (DescriptorImpl.ALLOWED_TYPES.contains(type)) {
            this.type = type;
        }
    }

    public String getType() {
        return type;
    }

    public boolean isMultiConfigMode() {
        return isType(DescriptorImpl.MULTI_TYPE);
    }

    public boolean isAutoMatch() {
        return !isMultiConfigMode() && baseConfig.isAutoMatch();
    }

    @DataBoundSetter
    public void setAutoMatch(boolean autoMatch) {
        if (!isMultiConfigMode()) {
            baseConfig.setAutoMatch(autoMatch);
        }
    }

    @DataBoundSetter
    public void setSubJobConfigs(Collection<SubJobConfig> subJobConfigs) {
        if (subJobConfigs != null && !subJobConfigs.isEmpty()) {
            this.subJobConfigs = new LinkedList<>(subJobConfigs);
        } else {
            this.subJobConfigs = new LinkedList<>();
            this.subJobConfigs.add(new SubJobConfig());
        }
    }

    public boolean isPathCorrectionNeeded() {
        return isAutoMatch();
    }

    public String getPullrequestKey() {
        return pullrequestKey;
    }

    @DataBoundSetter
    public void setPullrequestKey(String pullrequestKey) {
        this.pullrequestKey = pullrequestKey;
    }

    public String getComponent() {
        return component;
    }

    @DataBoundSetter
    public void setComponent(String component) {
        this.component = component;
    }

    public List<SonarInstallation> getSonarInstallations() {
        SonarGlobalConfiguration sonarGlobalConfiguration = GlobalConfiguration.all().get(SonarGlobalConfiguration.class);
        return sonarGlobalConfiguration != null ? Arrays.asList(sonarGlobalConfiguration.getInstallations()) : null;
    }

    @DataBoundSetter
    public void setSonarInstallationName(String sonarInstallationName) {
        this.sonarInstallationName = sonarInstallationName;
    }

    public String getSonarInstallationName() {
        return sonarInstallationName;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<InspectionConfig> {
        public static final String SONAR_URL = SonarToGerritPublisher.DescriptorImpl.SONAR_URL;
        public static final String SONAR_PULLREQUEST_KEY = SonarToGerritPublisher.DescriptorImpl.SONAR_PULLREQUEST_KEY;
        public static final String BASE_TYPE = "base";
        public static final String MULTI_TYPE = "multi";
        public static final String SQ7_TYPE = "sq7";
        public static final String DEFAULT_INSPECTION_CONFIG_TYPE = SonarToGerritPublisher.DescriptorImpl.DEFAULT_INSPECTION_CONFIG_TYPE;
        public static final boolean AUTO_MATCH = SonarToGerritPublisher.DescriptorImpl.AUTO_MATCH_INSPECTION_AND_REVISION_PATHS;

        private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(BASE_TYPE, MULTI_TYPE, SQ7_TYPE));

        /** Is only called once, filtering is done in Frontend by Combo Box */
        @SuppressWarnings("unused")
        public ComboBoxModel doFillComponentItems(@QueryParameter String value, @QueryParameter String sonarInstallationName) throws AbortException {
            SonarClient sonarClient = SonarUtil.getSonarClient(sonarInstallationName);
            ComponentSearchResult componentSearchResult = sonarClient.fetchComponent(value);

            return new ComboBoxModel(componentSearchResult.getComponents().stream()
                    .map(c -> c.getName() + " (" + c.getKey() + ")")
                    .collect(Collectors.toList()));
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckComponent(@QueryParameter String value, @QueryParameter String sonarInstallationName) throws AbortException {
            SonarClient sonarClient = SonarUtil.getSonarClient(sonarInstallationName);
            String componentKey = SonarUtil.isolateComponentKey(value);
            ComponentSearchResult componentSearchResult = sonarClient.fetchComponent(componentKey);

            if (componentSearchResult.getPaging().getTotal() == 1) {
                Component component = componentSearchResult.getComponents().get(0);
                if (!Objects.equals(componentKey, component.getKey())) {
                    return FormValidation.error("Ambiguous project key '" + value + "'. Did you mean '" + component.getKey() + "'?");
                } else {
                    return FormValidation.ok(component.getName() + ": " + sonarClient.getServerUrl() + "dashboard?id=" + component.getKey());
                }
            } else if (componentSearchResult.getPaging().getTotal() > 1) {
                return FormValidation.error("Multiple results found for '" + componentKey + "' on " + sonarClient.getServerUrl());
            } else {
                return FormValidation.error("'" + componentKey + "' could not be found on " + sonarClient.getServerUrl());
            }
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "InspectionConfig";
        }
    }


}
