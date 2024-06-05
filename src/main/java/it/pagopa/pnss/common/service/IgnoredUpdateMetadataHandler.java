package it.pagopa.pnss.common.service;

import java.util.Set;

public interface IgnoredUpdateMetadataHandler {

    boolean isToIgnore(String fileKey);

    void addFileKey(String fileKey);

    void removeFileKey(String fileKey);

    int updateSet(Set<String> newSet);

    int getSetSize();

}
