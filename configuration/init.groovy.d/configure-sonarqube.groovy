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

def disableSonar = System.getenv("DISABLE_SONAR");
if(disableSonar != null && disableSonar.toUpperCase() == "TRUE") {
    LOG.log(Level.INFO, 'Skipping SonarQube configuration')
    return
}

LOG.log(Level.INFO, 'Configuring SonarQube')
def sonarConfig = Jenkins.instance.getDescriptor('hudson.plugins.sonar.SonarGlobalConfiguration')

def sonarHost = System.getenv("SONARQUBE_URL");
if(sonarHost == null) {
    //default
    sonarHost = "http://sonarqube:9000"
}

def tokenName = 'Jenkins'

// Make a POST request to delete any existing admin tokens named "Jenkins"
LOG.log(Level.INFO, 'Delete existing SonarQube Jenkins token')
def revokeToken = new URL("${sonarHost}/api/user_tokens/revoke").openConnection()
def message = "name=Jenkins&login=admin"
revokeToken.setRequestMethod("POST")
revokeToken.setDoOutput(true)
revokeToken.setRequestProperty("Accept", "application/json")
def authString = "admin:admin".bytes.encodeBase64().toString()
revokeToken.setRequestProperty("Authorization", "Basic ${authString}")

def rc = 0
try {
  revokeToken.getOutputStream().write(message.getBytes("UTF-8"))
  rc = revokeToken.getResponseCode()
} catch (Exception ex) {
   LOG.log(Level.WARNING, 'Error deleting token')
   LOG.log(Level.INFO, generateToken.getErrorStream().getText())
}
if (rc == 200) {
  LOG.log(Level.INFO, 'Delete existing token')
}

// Create a new admin token named "Jenkins" and capture the value
LOG.log(Level.INFO, 'Generate new auth token for SonarQube/Jenkins integration')
def generateToken = new URL("${sonarHost}/api/user_tokens/generate").openConnection()
message = "name=${tokenName}&login=admin"
generateToken.setRequestMethod("POST")
generateToken.setDoOutput(true)
generateToken.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
generateToken.setRequestProperty("Authorization", "Basic ${authString}")
def retryLimit = 5

// Wait for Sonar to come alive
while (retryLimit > 0)
{
  try {
    revokeToken.getOutputStream().write(message.getBytes("UTF-8"))
    rc = revokeToken.getResponseCode()
  } catch (Exception ex) {
    rc = 0
    LOG.log(Level.WARNING, 'Error deleting token')
    LOG.log(Level.INFO, revokeToken.getErrorStream().getText())
  }
  
  if (rc == 200) {
      retryLimit = 0
  } else {
      LOG.log(Level.INFO, 'Error getting SonarQube auth token will retry, rc:' + rc)
      retryLimit--
      sleep(60)
  }

}

if (rc == 200) {
    LOG.log(Level.INFO, 'Successfully generated SonarQube auth token')
    def jsonBody = generateToken.getInputStream().getText()
    def jsonParser = new JsonSlurper()
    def data = jsonParser.parseText(jsonBody)
    def token = hudson.util.Secret.fromString(data.token)

    def secretBytes = SecretBytes.fromBytes(data.token.getBytes())

    def secretText = new StringCredentialsImpl(
                          CredentialsScope.GLOBAL,
                          "sonar",
                          "sonar Text Description",
                           token)

    SystemCredentialsProvider.instance.store.addCredentials(Domain.global(), secretText)

    // Add the SonarQube server config to Jenkins
    SonarInstallation sonarInst = new SonarInstallation(
        "sonar", 
        sonarHost, 
        "sonar", 
        token, 
        "","","","", new TriggersConfig())
    sonarConfig.setInstallations(sonarInst)
    sonarConfig.setBuildWrapperEnabled(true)
    sonarConfig.save()

    // Sonar Runner
    // Source: http://pghalliday.com/jenkins/groovy/sonar/chef/configuration/management/2014/09/21/some-useful-jenkins-groovy-scripts.html
    def inst = Jenkins.getInstance()

    def sonarRunner = inst.getDescriptor("hudson.plugins.sonar.SonarRunnerInstallation")

    def installer = new SonarRunnerInstaller("3.0.3.778")
    def prop = new InstallSourceProperty([installer])
    def sinst = new SonarRunnerInstallation("sonar-scanner-tool", "", [prop])
    sonarRunner.setInstallations(sinst)

    sonarRunner.save()

    LOG.log(Level.INFO, 'SonarQube configuration complete')
} else {
    LOG.log(Level.WARNING, "Request failed: ${rc}")
    LOG.log(Level.INFO, generateToken.getErrorStream().getText())
}
