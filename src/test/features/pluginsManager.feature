Feature: Plugins manager for plugin replication.

  Scenario: Plugin configuration files are all bundled into a map
    Given: there are plugins installed in jenkins directory
    When: user sends these files in a map with key as plugin name 
    And: plugin configuration content is stored as map value
    Then: each plugin configuration sent as map replaces existing configuration

  Scenario: Plugin configuration information is persisted from byte array 
    when plugin configuration is already loaded by jenkins
    Given: there is a byte array representing configuration file information provided as parameter
    And: there is a pluginName provided as parameter
    And: there is a fileName provided as parameter
    And: plugin configuration is already loaded by jenkins
    When: plugin configuration update command is triggered
    Then: updated plugin configuration information is persisted to memory

  Scenario: Plugin configuration information is persisted from byte array
  when plugin configuration is not loaded by jenkins perviously
    Given: there is a byte array representing configuration file information provided as parameter
    And: there is a pluginName provided as parameter
    And: there is a fileName provided as parameter
    And: plugin configuration is not loaded by jenkins
    When: plugin configuration update command is triggered
    Then: updated plugin configuration information is persisted to file
