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
 */
package org.nuxeo.ecm.platform.wi.backend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractCoreBackend implements Backend {

    private static final Log log = LogFactory.getLog(AbstractCoreBackend.class);

    protected CoreSession session;

    public AbstractCoreBackend() {
        super();
    }

    protected AbstractCoreBackend(CoreSession session) {
        this.session = session;
    }

    @Override
    public CoreSession getSession() {
        return getSession(false);
    }

    @Override
    public CoreSession getSession(boolean synchronize) {
        try {
            if (session == null) {
                RepositoryManager rm;
                rm = Framework.getService(RepositoryManager.class);
                session = rm.getDefaultRepository().open();
            }
            if (synchronize) {
                session.save();
            }
        } catch (Exception e) {
            throw new ClientRuntimeException("Error while getting session", e);
        }
        return session;
    }

    @Override
    public void setSession(CoreSession session) {
        this.session = session;
    }

    @Override
    public boolean isSessionAlive() {
        try {
            getSession().getPrincipal();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void destroy() {
        close();
    }

    protected void close() {
        if (session != null) {
            CoreInstance.getInstance().close(session);
            session = null;
        }
    }

    @Override
    public void discardChanges() throws ClientException {
        discardChanges(false);
    }

    public void discardChanges(boolean release) throws ClientException {
        // TransactionHelper.setTransactionRollbackOnly();
        try {
            if (session != null) {
                try {
                    session.cancel();
                    if (release) {
                        close();
                    }
                } catch (Exception e) {
                    throw new ClientException("Error during discard", e);
                }
            }
        } finally {
            // TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Override
    public void saveChanges() throws ClientException {
        saveChanges(false);
    }

    public void saveChanges(boolean release) throws ClientException {
        try {
            if (session != null) {
                try {
                    session.save();
                    if (release) {
                        close();
                    }
                } catch (ClientException e) {
                    throw new ClientException("Error during save", e);
                }
            }
        } finally {
            // TransactionHelper.commitOrRollbackTransaction();
        }

    }

}
