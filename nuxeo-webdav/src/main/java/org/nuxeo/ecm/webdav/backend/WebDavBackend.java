package org.nuxeo.ecm.webdav.backend;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.*;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Organization: Gagnavarslan ehf
 */
public interface WebDavBackend {

    void saveChanges() throws ClientException;

    void discardChanges() throws ClientException;

    boolean isLocked(DocumentRef ref) throws ClientException;

    boolean canUnlock(DocumentRef ref) throws ClientException;

    String lock(DocumentRef ref) throws ClientException;

    boolean unlock(DocumentRef ref) throws ClientException;

    String getCheckoutUser(DocumentRef ref) throws ClientException;

    Path parseLocation(String location);

    DocumentModel resolveLocation(String location) throws ClientException;

    void removeItem(String location) throws ClientException;

    void removeItem(DocumentRef ref) throws ClientException;

    void renameItem(DocumentModel source, String destinationName) throws ClientException;

    DocumentModel moveItem(DocumentModel source, PathRef targetParentRef) throws ClientException;

    DocumentModel copyItem(DocumentModel source, PathRef targetParentRef) throws ClientException;

    DocumentModel createFolder(String parentPath, String name) throws ClientException;

    DocumentModel createFile(String parentPath, String name, Blob content) throws ClientException;

    DocumentModel createFile(String parentPath, String name) throws ClientException;

    DocumentModel saveDocument(DocumentModel doc) throws ClientException;

    List<DocumentModel> getChildren(DocumentRef ref) throws ClientException;

    boolean isRename(String source, String destination);

    boolean exists(String location);

    boolean hasPermission(DocumentRef docRef, String permission) throws ClientException;

    String getDisplayName(DocumentModel doc);

    LinkedList<String> getVirtualFolderNames() throws ClientException;

    boolean isVirtual();
}