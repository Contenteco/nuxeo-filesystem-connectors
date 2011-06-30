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
package org.nuxeo.ecm.platform.wi.filter;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WISession implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String BACKEND_KEY = "org.nuxeo.ecm.platform.wi.backend";

    public static final String CORESESSION_KEY = "org.nuxeo.ecm.platform.wi.coresession";

    private Map<String, Object> attributes = new HashMap<String, Object>();

    private String key;

    private long creationTime;

    private long accessTime;

    private long accessValidTime;

    private long invalidLifeTime;

    private boolean invalid = false;

    public WISession(String key, int accessValidTime, int invalidLifeTime) {
        this.creationTime = System.currentTimeMillis();
        access();
        this.key = key;
        this.accessValidTime = accessValidTime * 1000L;
        this.invalidLifeTime = invalidLifeTime * 1000L;
    }

    public void reload(){
        this.creationTime = System.currentTimeMillis();
        access();
        attributes = new HashMap<String, Object>();
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    public Collection<Object> getAttributes() {
        return attributes.values();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void access() {
        this.accessTime = System.currentTimeMillis();
    }

    public void invalid(){
        invalid = true;
    }

    public boolean isValid() {
        long time = System.currentTimeMillis();
        if ((invalid && time > accessTime + accessValidTime) ||
                (invalidLifeTime != 0
                        && time > creationTime + invalidLifeTime
                        && time > accessTime + accessValidTime)) {
            
            return false;
        } else {
            return true;
        }
    }
}
