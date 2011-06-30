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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.wi.backend.Backend;

public class SessionCache implements Serializable {

    private static final Log log = LogFactory.getLog(SessionCache.class);

    /*
     Session can't be invalidate if last access was less then this time.
     Time in seconds. Default value.
     */
    private static final int DEFAULT_ACCESS_VALID_TIME = 30;

    /*
     Session will be invalidate if its lifetime more than this value.
     Time in seconds. Default value.
     */
    private static final int DEFAULT_INVALID_LIFE_TIME = 20 * 60;

    private static final long serialVersionUID = 1L;

    protected static final String COMMA_SEPARATOR = ",";

    protected static final String EQUAL_SEPARATOR = "=";

    protected static final String QUOTE = "\"";

    protected int accessValidTime = DEFAULT_ACCESS_VALID_TIME;

    protected int invalidLifeTime = DEFAULT_INVALID_LIFE_TIME;

    private Map<String, WISession> map = new ConcurrentHashMap<String, WISession>();

    public WISession get(HttpServletRequest httpRequest) {
        String key = getKey(httpRequest);
        WISession session = get(key);
        return session;
    }

    public WISession get(String key) {
        if (StringUtils.isEmpty(key)) {
            WISession session = new WISession(key, accessValidTime, invalidLifeTime);
            return session;
        }
        WISession session = map.get(key);
        if (session == null) {
            session = new WISession(key, accessValidTime, invalidLifeTime);
            put(session);
            return session;
        }

        if (session.isValid()) {
            session.access();
            return session;
        } else {
            Backend backend = (Backend) session.getAttribute(WIRequestFilter.BACKEND_KEY);
            CoreSession coreSession = null;
            if (backend != null) {
                if (backend.isSessionAlive()) {
                    coreSession = backend.getSession();
                }
            }
            session.reload();
            if (coreSession != null) {
                session.setAttribute(WISession.CORESESSION_KEY, coreSession);
            }
            put(session);
            return session;
        }
    }

    public void put(WISession session) {
        map.put(session.getKey(), session);
    }

    public void invalidateCache() {
        log.warn("Catching invalidate WSS/WebDAV cache event.");
        for (WISession session : map.values()) {
            session.invalid();
        }
    }

    public void setAccessValidTime(int accessValidTime) {
        this.accessValidTime = accessValidTime;
    }

    public void setInvalidLifeTime(int invalidLifeTime) {
        this.invalidLifeTime = invalidLifeTime;
    }

    private String getKey(HttpServletRequest httpRequest) {
        String key = "";
        String header = httpRequest.getHeader("Authorization");
        if (StringUtils.isEmpty(header)) {
            return "";
        }
        String username = null;
        if (header.toLowerCase().startsWith("digest")) {
            int idx = header.indexOf(' ');
            Map<String, String> headerMap = splitResponseParameters(header.substring(idx + 1));
            username = headerMap.get("username");
        } else if (header.toLowerCase().startsWith("basic")) {
            int idx = header.indexOf(' ');
            String b64userpassword = header.substring(idx + 1);
            byte[] clearUp = Base64.decode(b64userpassword);
            String userpassword = new String(clearUp);
            String[] up = userpassword.split(":");
            if (up.length == 2) {
                username = up[0];
            }
        }
        if (StringUtils.isNotEmpty(username)) {
            key = username;
        }
        if (StringUtils.isEmpty(key)) {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                key = session.getId();
            }
        }
        return key;
    }

    private Map<String, String> splitResponseParameters(String auth) {
        String[] array = auth.split(COMMA_SEPARATOR);

        if ((array == null) || (array.length == 0)) {
            return null;
        }

        Map<String, String> map = new HashMap<String, String>();
        for (String item : array) {

            item = StringUtils.replace(item, QUOTE, "");
            String[] parts = item.split(EQUAL_SEPARATOR);

            if (parts == null) {
                continue;
            }

            map.put(parts[0].trim(), item.substring(parts[0].length() + 1));
        }

        return map;
    }

}
