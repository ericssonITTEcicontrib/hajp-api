Feature: Builds manager for build replication.
  Freestyle build files are zipped/unzipped for replication purposes

  Scenario: Grabbing a build files tree as a byte array
    Given: there is a freestyle job directory existing under the jenkins jobs directory
    And: there is a build along with its files exists under the job directory
    When: the build files tree is grabbed
    Then: the builds files tree is zipped and converted to a byt array

  Scenario: Build files are unzipped from a byte array
    Given: there is a zip file as an array that contains a build files tree
    When: that file is extracted
    Then: the builds files tree is unzipped properly
