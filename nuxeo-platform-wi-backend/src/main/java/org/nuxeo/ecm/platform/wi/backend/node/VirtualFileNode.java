package org.nuxeo.ecm.platform.wi.backend.node;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webdav.backend.VirtualNode;

/**
 * Date: 09.06.2011
 * Time: 13:46:55
 *
 * @author Vitalii Siryi
 */
public class VirtualFileNode implements VirtualNode {

    private String name;
    private long size = 0;
    private String mimeType = "";

    public VirtualFileNode(String name, long size, String mimeType) {
        this.name = name;
        this.size = size;
        this.mimeType = mimeType;
    }

    public boolean isFolder() {
        return false;
    }

    public String getName() {
        return this.name;
    }

    public long getSize() {
        return this.size;
    }

    public String getMimeType() {
        return this.mimeType;
    }
}
