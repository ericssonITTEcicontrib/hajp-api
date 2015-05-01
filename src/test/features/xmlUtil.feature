Feature: Xml utility for xml merging while preserving some fields.
  Upon receiving a new Xml, the current xml is overwritten with new the new xml document except for
  fields that referenced in the preserved fields xml.

  Scenario: Xml overwritten completely if there is no preserved field reference
    Given: there is an original freestyle job config xml
    And: there is a received freestyle job config xml
    And: the preserved fields xml has no fields referenced for jobs
    When: the 2 config xml are merging
    Then: the resulting xml is completely overwritten by the received config xml

  Scenario: Xml overwritten except for a field matching the preserved field reference
    Given: there is an original freestyle job config xml containing a credential field
    And: there is a received freestyle job config xml containing a credential field
    And: the preserved fields xml has a credential field referenced for jobs
    When: the 2 config xml are merging
    Then: the resulting xml is overwritten with the received xml document
  except for the credential field preserving the value from the original xml

  Scenario: Xml  overwritten except for multiple fields matching the preserved field reference
    Given: there is an original freestyle job config xml containing multiple credential field
    And: there is a received freestyle job config xml containing multiple credential field
    And: the preserved fields xml has a credential field referenced for jobs
    When: the 2 config xml are merging
    Then: the resulting xml is overwritten with the received xml document
  except for the credential fields preserving the values from the original xml
