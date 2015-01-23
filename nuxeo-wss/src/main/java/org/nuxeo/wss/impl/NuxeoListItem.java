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
 *     Thierry Delprat
 *     Gagnavarslan ehf
 *     Florent Guillaume
 */
package org.nuxeo.wss.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.spi.AbstractWSSListItem;
import org.nuxeo.wss.spi.WSSListItem;

public class NuxeoListItem extends AbstractWSSListItem implements WSSListItem {

    private static final Log log = LogFactory.getLog(NuxeoListItem.class);

    protected DocumentModel doc;

    protected String corePathPrefix;

    protected String urlRoot;

    protected String virtualName = null;

    protected String virtualRootNodeName = null;

    protected CoreSession getSession() {
        return doc.getCoreSession();
    }

    public NuxeoListItem(DocumentModel doc, String corePathPrefix, String urlRoot) {
        this.doc = doc;
        this.corePathPrefix = corePathPrefix;
        this.urlRoot = urlRoot;
    }

    @Override
    protected Date getCheckoutDate() {
        Lock lock = null;
        try {
            lock = getSession().getLockInfo(doc.getRef());
        } catch (ClientException e) {
            log.error("Unable to get lock", e);
        }
        if (lock != null) {
            return lock.getCreated().getTime();
        }
        return Calendar.getInstance().getTime();
    }

    @Override
    protected Date getCheckoutExpiryDate() {
        Date to = getCheckoutDate();
        if (to != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(to);
            cal.add(Calendar.MINUTE, 20);
            return cal.getTime();
        }
        return Calendar.getInstance().getTime();
    }

    @Override
    public Date getCreationDate() {
        try {
            Calendar modified = (Calendar) doc.getPropertyValue("dc:created");
            if (modified != null) {
                return modified.getTime();
            }
        } catch (ClientException e) {
            log.error("Unable to get creation date", e);
        }
        return Calendar.getInstance().getTime();
    }

    @Override
    public Date getModificationDate() {
        try {
            Calendar modified = (Calendar) doc.getPropertyValue("dc:modified");
            if (modified != null) {
                return modified.getTime();
            }
        } catch (ClientException e) {
            log.error("Unable to get modification date", e);
        }
        return Calendar.getInstance().getTime();
    }

    @Override
    public void checkOut(String userName) throws WSSException {
        try {
            Lock lock = getSession().getLockInfo(doc.getRef());
            if (lock == null) {
                getSession().setLock(doc.getRef());
            } else {
                if (!userName.equals(getCheckoutUser())) {
                    throw new WSSException("Document is already locked by another user");
                }
            }
        } catch (ClientException e) {
            throw new WSSException("Error while locking", e);
        }
    }

    @Override
    public String getCheckoutUser() {
        Lock lock = null;
        try {
            lock = getSession().getLockInfo(doc.getRef());
        } catch (ClientException e) {
            log.error("Unable to get lock", e);
        }
        if (lock != null) {
            return lock.getOwner();
        }
        return null;
    }

    @Override
    public String getDescription() {
        try {
            return (String) doc.getPropertyValue("dc:description");
        } catch (PropertyException e) {
            log.error("Unable to get description", e);
            return "";
        }
    }

    @Override
    public String getEtag() {
        return doc.getId();
    }

    @Override
    public String getLastModificator() {
        return null;
    }

    @Override
    public String getName() {
        if (virtualName != null) {
            return virtualName;
        }
        return doc.getName();
    }

    @Override
    public int getSize() {
        int size = 0;
        if (!doc.isFolder()) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                try {
                    Blob blob = bh.getBlob();
                    if (blob != null) {
                        size = (int) blob.getLength();
                    }
                } catch (ClientException e) {
                    log.error("Unable to get blob Size", e);
                }
            }
        }
        if (size == 0) {
            size = 10;
        }
        return size;
    }

    @Override
    public InputStream getStream() {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            try {
                Blob blob = bh.getBlob();
                if (blob != null) {
                    return blob.getStream();
                }
            } catch (ClientException | IOException e) {
                log.error("Unable to get Stream", e);
            }
        }
        return null;
    }

    @Override
    public String getSubPath() {

        String path = doc.getPathAsString();
        if (corePathPrefix != null) {
            path = path.replace(corePathPrefix, "");
        }
        if (virtualName != null) {
            Path vPath = new Path(path);
            vPath = vPath.removeFirstSegments(1);
            path = new Path(virtualName).append(vPath).toString();
        } else if (virtualRootNodeName != null) {
            path = new Path(virtualRootNodeName).append(path).toString();
        }
        Path completePath = new Path(urlRoot);
        completePath = completePath.append(path);
        path = completePath.toString();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    @Override
    public String getRelativeFilePath(String siteRootPath) {
        String path = getRelativeSubPath(siteRootPath);
        if (!doc.isFolder()) {
            String filename = getFileName();
            if (filename != null) {
                // XXX : check for duplicated names
                path = new Path(path).removeLastSegments(1).append(filename).toString();
            }
        }
        return path;
    }

    protected String getFileName() {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            try {
                Blob blob = bh.getBlob();
                if (blob != null) {
                    return blob.getFilename();
                }
            } catch (ClientException e) {
                log.error("Unable to get filename", e);
            }
        }
        return null;
    }

    @Override
    public String getType() {
        if (doc.isFolder()) {
            return "folder";
        }
        return "file";
    }

    @Override
    public void setDescription(String description) {
        try {
            doc.setPropertyValue("dc:description", description);
        } catch (PropertyException e) {
            log.error("Error while setting description", e);
        }
    }

    @Override
    public void setStream(InputStream is, String fileName) throws WSSException {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            try {
                Blob blob = new FileBlob(is);
                if (fileName != null) {
                    blob.setFilename(fileName);
                }
                Blob oldBlob = bh.getBlob();
                if (oldBlob == null) {
                    // force to recompute icon
                    if (doc.hasSchema("common")) {
                        doc.setProperty("common", "icon", null);
                    }
                }
                bh.setBlob(blob);
                doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
                doc = getSession().saveDocument(doc);
            } catch (ClientException | IOException e) {
                log.error("Error while setting stream", e);
            }
        } else {
            log.error("Update of type " + doc.getType() + " is not supported for now");
        }
    }

    @Override
    public void uncheckOut(String userName) throws WSSException {
        try {
            Lock lock = getSession().getLockInfo(doc.getRef());
            if (lock != null) {
                if (userName.equals(lock.getOwner())) {
                    getSession().removeLock(doc.getRef());
                } else {
                    throw new WSSException("Document is locked by another user");
                }
            }
        } catch (ClientException e) {
            throw new WSSException("Error while unlocking", e);
        }
    }

    @Override
    public String getDisplayName() {
        if (doc.isFolder()) {
            try {
                return doc.getTitle();
            } catch (ClientException e) {
                return getName();
            }
        } else {
            String fileName = getFileName();
            if (fileName == null) {
                fileName = getName();
            }
            return fileName;
        }
    }

    @Override
    protected String getExtension() {
        String fileName = getFileName();
        if (fileName == null) {
            return super.getExtension();
        } else {
            return new Path(fileName).getFileExtension();
        }
    }

    public void setVirtualName(String virtualName) {
        this.virtualName = virtualName;
    }

    public void setVirtualRootNodeName(String virtualRootNodeName) {
        this.virtualRootNodeName = virtualRootNodeName;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    @Override
    public String getAuthor() {
        try {
            return (String) doc.getPropertyValue("dc:creator");
        } catch (PropertyException e) {
            return "";
        }
    }

    @Override
    protected String getIconFromType() {
        if ("Workspace".equals(doc.getType())) {
            return "workspace.gif";
        } else {
            return super.getIconFromType();
        }
    }

    @Override
    public boolean isFolderish() {
        return doc.isFolder();
    }

    @Override
    public boolean canCheckOut(String userName) {
        boolean canCheckOut = super.canCheckOut(userName);
        if (canCheckOut) {
            try {
                return getSession().hasPermission(doc.getRef(), "WriteProperties");
            } catch (ClientException e) {
                log.error("Error during permission check", e);
                return false;
            }
        }
        return canCheckOut;
    }

    @Override
    public boolean canUnCheckOut(String userName) {
        boolean canUnCheckOut = super.canUnCheckOut(userName);
        if (canUnCheckOut) {
            try {
                return getSession().hasPermission(doc.getRef(), "WriteProperties");
            } catch (ClientException e) {
                log.error("Error during permission check", e);
                return false;
            }
        }
        return canUnCheckOut;
    }

}
