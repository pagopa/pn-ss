Feature: Upload SafeStorage
#CASE TEST KO
  #in cima perch� "it's available rimane in pending a causa della lambda che non cambia stato al file

  Scenario Outline: Upload di un file non sottoposto a trasformazione con un clientId non riconosciuto
    Given "<clientId>" authenticated by "<APIKey>" try to upload a document of type "<documentType>" with content type "<MIMEType>" using "<fileName>"
    When request a presigned url to upload the file
    Then i get an error "<rc>"
    Examples:
      | clientId    | APIKey              | documentType                | fileName    | MIMEType        | rc |
      | pn-unkown | pn-delivery_api_key | PN_NOTIFICATION_ATTACHMENTS | src/test/resources/test.zip | application/zip | 403 |


  Scenario Outline: Upload di un file da sottoporre a trasformazione con un clientId non riconosciuto
    Given "<clientId>" authenticated by "<APIKey>" try to upload a document of type "<documentType>" with content type "<MIMEType>" using "<fileName>"
    When request a presigned url to upload the file
    Then i get an error "<rc>"
    Examples:
      | clientId    | APIKey              | documentType                | fileName    | MIMEType        | rc |
      | pn-unkown | pn-delivery_api_key | PN_NOTIFICATION_ATTACHMENTS | src/test/resources/test.zip | application/zip | 403 |

  Scenario Outline: Upload di un file da sottoporre a trasformazione senza xTraceId come header
    Given "<clientId>" authenticated by "<APIKey>" try to upload a document of type "<documentType>" with content type "<MIMEType>" using "<fileName>"
    When request a presigned url to upload the file without traceId
    Then i get an error "<rc>"
    Examples:
      | clientId    | APIKey              | documentType                | fileName    | MIMEType        | rc |
      | pn-delivery | pn-delivery_api_key | PN_NOTIFICATION_ATTACHMENTS | src/test/resources/test.zip | application/zip | 400 |


  Scenario Outline: Upload di un file non sottoposto a trasformazione senza xTraceId come header
    Given "<clientId>" authenticated by "<APIKey>" try to upload a document of type "<documentType>" with content type "<MIMEType>" using "<fileName>"
    When request a presigned url to upload the file without traceId
    Then i get an error "<rc>"
    Examples:
      | clientId    | APIKey              | documentType                | fileName    | MIMEType        | rc |
      | pn-delivery | pn-delivery_api_key | PN_NOTIFICATION_ATTACHMENTS | src/test/resources/test.zip | application/zip | 400 |

# END TEST KO

  Scenario Outline: Upload di un file non sottoposto a trasformazione
    Given "<clientId>" authenticated by "<APIKey>" try to upload a document of type "<documentType>" with content type "<MIMEType>" using "<fileName>"
    When request a presigned url to upload the file
    And upload that file
    Then i found in S3
		Examples:
      | clientId    | APIKey              | documentType                | fileName    | MIMEType        |
      | pn-delivery | pn-delivery_api_key | PN_NOTIFICATION_ATTACHMENTS | src/test/resources/test.zip | application/zip |
	    | pn-delivery | pn-delivery_api_key | PN_NOTIFICATION_ATTACHMENTS | src/test/resources/test.pdf | application/pdf |

  Scenario Outline: Casi di errore in fase di richiesta della presigned URL di upload
    Given "<clientId>" authenticated by "<APIKey>" try to upload a document of type "<documentType>" with content type "<MIMEType>" using "<fileName>"
    When request a presigned url to upload the file
    Then i get an error "<rc>"
		Examples:
      | clientId    | APIKey              | documentType   | fileName    | MIMEType        | rc  |
      | pn-delivery | pn-delivery_api_key | PN_LEGAL_FACTS | src/test/resources/test.zip | application/zip | 403 |

  Scenario Outline: Upload di un file da sottoporre a trasformazione
    Given "<clientId>" authenticated by "<APIKey>" try to upload a document of type "<documentType>" with content type "<MIMEType>" using "<fileName>"
    When request a presigned url to upload the file
    And upload that file
    And it's available
    Then i found in S3
		Examples:
      | clientId | APIKey          | documentType      | fileName    | MIMEType        |
      | pn-test  | pn-test_api_key | PN_LEGAL_FACTS_ST | src/test/resources/test.zip| application/zip |
	  | pn-test  | pn-test_api_key | PN_LEGAL_FACTS_ST | src/test/resources/test.pdf | application/pdf |




