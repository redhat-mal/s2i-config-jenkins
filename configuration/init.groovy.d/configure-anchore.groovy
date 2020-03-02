#!/usr/bin/env groovy
import jenkins.model.*
import groovy.json.JsonSlurper
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.SonarRunnerInstallation
import hudson.plugins.sonar.SonarRunnerInstaller
import hudson.plugins.sonar.model.TriggersConfig
import hudson.tools.InstallSourceProperty
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.Domain;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;


import java.util.logging.Level
import java.util.logging.Logger

final def LOG = Logger.getLogger("LABS")

LOG.log(Level.INFO, 'Configuring Anchore')


def token = hudson.util.Secret.fromString("anchore")

def secretText = new StringCredentialsImpl(
                          CredentialsScope.GLOBAL,
                          "anchore-creds",
                          "Anchore API Access",
                           token)

SystemCredentialsProvider.instance.store.addCredentials(Domain.global(), secretText)

    // Add the SonarQube server config to Jenkins

LOG.log(Level.INFO, 'Anchore configuration complete')

