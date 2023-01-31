package it.pagopa.pnss.common.client.dto;


import java.util.List;

public class UserConfigurationDTO {

    private String name;
    private List<String> canCreate;
    private List<String> canRead;
    private String signatureInfo;
    private UserConfigurationDestinationDTO destination;
    private String apiKey;

    public String getName() {
        return name;
    }
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    public void setName(String name) {
        this.name = name;
    }

    public List<String> getCanCreate() {
        return canCreate;
    }

    public void setCanCreate(List<String> canCreate) {
        this.canCreate = canCreate;
    }

    public List<String> getCanRead() {
        return canRead;
    }

    public void setCanRead(List<String> canRead) {
        this.canRead = canRead;
    }


    public String getSignatureInfo() {
        return signatureInfo;
    }

    public void setSignatureInfo(String signatureInfo) {
        this.signatureInfo = signatureInfo;
    }

    public UserConfigurationDestinationDTO getDestination() {
        return destination;
    }

    public void setDestination(UserConfigurationDestinationDTO destination) {
        this.destination = destination;
    }


}
