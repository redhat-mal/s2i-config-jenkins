#!/usr/bin/env groovy
import jenkins.model.*
import groovy.json.JsonSlurper
import hudson.plugins.sonar.model.TriggersConfig
import hudson.tools.InstallSourceProperty
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.*


import java.util.logging.Level
import java.util.logging.Logger

final def LOG = Logger.getLogger("LABS")

LOG.log(Level.INFO, 'Configuring Anchore')

anchoreUsername = System.getenv("ANCHORE_USERNAME") ?: "admin"
anchorePassword = System.getenv("ANCHORE_PASSWORD") ?: "anchore"

usernameAndPassword = new UsernamePasswordCredentialsImpl(
  CredentialsScope.GLOBAL,
  "anchore-creds", "Anchore creds for Jenkins",
  anchoreUsername,
  anchorePassword
)

SystemCredentialsProvider.instance.store.addCredentials(Domain.global(), usernameAndPassword)

    // Add the SonarQube server config to Jenkins

LOG.log(Level.INFO, 'Anchore configuration complete')

