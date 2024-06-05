package it.pagopa.pnss.common.service.impl;

import it.pagopa.pnss.common.service.IgnoredUpdateMetadataHandler;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A component class to handle those files that should be ignored during S3 updateSet metadata.
 */
@Service
@CustomLog
public class IgnoredUpdateMetadataHandlerImpl implements IgnoredUpdateMetadataHandler {

    private final ConcurrentHashMap.KeySetView<String, Boolean> ignoredUpdateMetadataSet;

    /**
     * Instantiates a new IgnoreUpdateMetadataHandler.
     */
    @Autowired
    public IgnoredUpdateMetadataHandlerImpl() {
        this.ignoredUpdateMetadataSet = ConcurrentHashMap.newKeySet();
    }

    /**
     * It checks if the file with the given fileKey should be ignored during S3 updateSet metadata.
     *
     * @param fileKey the file key
     * @return the boolean
     */
    public boolean isToIgnore(String fileKey) {
        return ignoredUpdateMetadataSet.contains(fileKey);
    }

    /**
     * Add a fileKey to the set containing those files that should be ignored.
     *
     * @param fileKey the file key
     */
    public void addFileKey(String fileKey) {
        ignoredUpdateMetadataSet.add(fileKey);
    }

    /**
     * Remove a fileKey from the set containing those files that should be ignored.
     *
     * @param fileKey the file key to remove
     */
    @Override
    public void removeFileKey(String fileKey) {
        ignoredUpdateMetadataSet.remove(fileKey);
    }

    /**
     * Updates the ignoredUpdateMetadataSet, removing those fileKeys that are not present in the new set, and adding the new ones.
     *
     * @param newSet the new set
     * @return the size of the updated set
     */
    public int updateSet(Set<String> newSet) {
        ignoredUpdateMetadataSet.retainAll(newSet);
        ignoredUpdateMetadataSet.addAll(newSet);
        return ignoredUpdateMetadataSet.size();
    }
}
