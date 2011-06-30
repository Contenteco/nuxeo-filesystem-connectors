package org.nuxeo.ecm.platform.wi.backend.node;

import org.nuxeo.ecm.webdav.backend.VirtualNode;

/**
 * Date: 09.06.2011
 * Time: 13:47:18
 *
 * @author Vitalii Siryi
 */
public class VirtualFolderNode implements VirtualNode {

    private String name;

    public VirtualFolderNode(String name) {
        this.name = name;
    }

    public boolean isFolder() {
        return true;
    }

    public String getName() {
        return this.name;
    }

    public long getSize() {
        return 0;
    }

    public String getMimeType() {
        return "";
    }
}
