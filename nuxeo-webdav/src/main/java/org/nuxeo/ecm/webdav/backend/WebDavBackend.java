/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.webdav.backend;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.*;

import java.util.LinkedList;
import java.util.List;
import org.nuxeo.ecm.core.security.SecurityException;

public interface WebDavBackend {

    void saveChanges() throws ClientException;

    void discardChanges() throws ClientException;

    boolean isLocked(DocumentRef ref) throws ClientException;

    boolean canUnlock(DocumentRef ref) throws ClientException;

    String lock(DocumentRef ref) throws ClientException;

    boolean unlock(DocumentRef ref) throws ClientException;

    String getCheckoutUser(DocumentRef ref) throws ClientException;

    Path parseLocation(String location);

    boolean removeItem(String location) throws ClientException, SecurityException;

    boolean removeItem(DocumentRef ref) throws ClientException;

    boolean renameItem(DocumentModel source, String destinationName) throws ClientException;

    DocumentModel moveItem(DocumentModel source, PathRef targetParentRef) throws ClientException;

    DocumentModel copyItem(DocumentModel source, PathRef targetParentRef) throws ClientException;

    DocumentModel createFolder(String parentPath, String name) throws ClientException;

    DocumentModel createFile(String parentPath, String name, Blob content) throws ClientException;

    DocumentModel createFile(String parentPath, String name) throws ClientException;

    DocumentModel updateDocument(DocumentModel doc, String name, Blob content) throws ClientException;

    List<DocumentModel> getChildren(DocumentRef ref) throws ClientException;

    boolean isRename(String source, String destination);

    boolean exists(String location);

    boolean hasPermission(DocumentRef docRef, String permission) throws ClientException;

    String getDisplayName(DocumentModel doc);

    LinkedList<VirtualNode> getVirtualNodes() throws ClientException;

    boolean isVirtual();

    DocumentModel getDocument(String location) throws Exception;
}
