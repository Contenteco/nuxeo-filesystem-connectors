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
package org.nuxeo.wss.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.webdav.backend.AbstractBackendFactory;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.backend.BackendHelper;
import org.nuxeo.ecm.webdav.backend.SearchBackendFactory;
import org.nuxeo.ecm.webdav.backend.SimpleBackendFactory;
import org.nuxeo.ecm.webdav.service.WIRequestFilter;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;

public class TestSimpleBackend extends SQLRepositoryTestCase {

    public static class WSSListItemSorter implements Comparator<WSSListItem> {
        @Override
        public int compare(WSSListItem a, WSSListItem b) {
            return a.getName().compareTo(b.getName());
        }
    }

    public static WSSListItemSorter wssListItemSorter = new WSSListItemSorter();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.webdav");
        fireFrameworkStarted();
        openSession();

        DocumentModel ws1 = session.createDocumentModel("/default-domain/workspaces", "ws1", "Workspace");
        ws1.setPropertyValue("dc:title", "Ws1");
        ws1 = session.createDocument(ws1);

        DocumentModel folder = session.createDocumentModel("/default-domain/workspaces/ws1", "folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder1");
        folder = session.createDocument(folder);

        DocumentModel isolatedws = session.createDocumentModel("/default-domain/workspaces/ws1/folder", "isolatedws",
                "Workspace");
        isolatedws.setPropertyValue("dc:title", "Isolatedws");
        isolatedws = session.createDocument(isolatedws);

        DocumentModel isolatedws2 = session.createDocumentModel("/default-domain/workspaces/ws1/folder", "ws2",
                "Workspace");
        isolatedws2.setPropertyValue("dc:title", "Isolatedws2");
        isolatedws2 = session.createDocument(isolatedws2);

        DocumentModel ws2 = session.createDocumentModel("/default-domain/workspaces", "ws2", "Workspace");
        ws2.setPropertyValue("dc:title", "Ws2");
        ws2 = session.createDocument(ws2);

        DocumentModel ws21 = session.createDocumentModel("/default-domain/workspaces/ws2", "ws21", "Workspace");
        ws21.setPropertyValue("dc:title", "Ws21");
        ws21 = session.createDocument(ws21);

        DocumentModel doc1 = session.createDocumentModel("/default-domain/workspaces/ws1", "doc1", "File");
        doc1.setPropertyValue("dc:title", "Doc1");
        Blob blob = Blobs.createBlob("Yo");
        blob.setFilename("document1.doc");
        doc1.setProperty("file", "content", blob);
        doc1 = session.createDocument(doc1);

        DocumentModel doc2 = session.createDocumentModel("/default-domain/workspaces/ws2", "doc2", "File");
        doc2.setPropertyValue("dc:title", "Doc2");
        blob = Blobs.createBlob("Yo");
        blob.setFilename("document2.doc");
        doc2.setProperty("file", "content", blob);
        doc2 = session.createDocument(doc2);

        DocumentModel doc3 = session.createDocumentModel("/default-domain/workspaces/ws2/ws21", "doc3", "File");
        doc3.setPropertyValue("dc:title", "Doc3");
        blob = Blobs.createBlob("Yo");
        blob.setFilename("document3.doc");
        doc3.setProperty("file", "content", blob);
        doc3 = session.createDocument(doc3);

        DocumentModel doc4 = session.createDocumentModel("/default-domain/workspaces/ws1/folder/isolatedws", "doc4",
                "File");
        doc4.setPropertyValue("dc:title", "Doc4");
        blob = Blobs.createBlob("Yo");
        blob.setFilename("document4.doc");
        doc4.setProperty("file", "content", blob);
        doc4 = session.createDocument(doc4);

        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    protected WSSBackend createWssBackend(AbstractBackendFactory factory) {
        BackendHelper.setBackendFactory(factory);
        Backend backend = factory.createRootBackend(session);
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getAttribute(WIRequestFilter.BACKEND_KEY)).thenReturn(backend);
        WSSRequest wssRequest = new WSSRequest(httpServletRequest, "/");
        return new WSSBackendFactoryImpl().getBackend(wssRequest);
    }

    @Test
    public void testSimpleBackendBrowse() throws Exception {
        WSSBackend backend = createWssBackend(new SimpleBackendFactory());

        List<WSSListItem> items = backend.listItems("/nuxeo");
        assertNotNull(items);
        assertEquals(3, items.size());

        items = backend.listItems("/nuxeo/workspaces");
        assertNotNull(items);
        assertEquals(2, items.size());
        Collections.sort(items, wssListItemSorter);
        assertEquals("ws1", items.get(0).getName());
        assertEquals("ws2", items.get(1).getName());
        assertEquals("Ws1", items.get(0).getDisplayName());
        assertEquals("Ws2", items.get(1).getDisplayName());

        assertEquals("nuxeo/workspaces/ws1", items.get(0).getSubPath());
        assertEquals("nuxeo/workspaces/ws1", items.get(0).getRelativeSubPath(""));
        assertEquals("workspaces/ws1", items.get(0).getRelativeSubPath("nuxeo"));

        items = backend.listItems("/nuxeo/workspaces/ws1");
        assertNotNull(items);
        assertEquals(2, items.size());
        Collections.sort(items, wssListItemSorter);
        assertEquals("doc1", items.get(0).getName());
        assertEquals("folder", items.get(1).getName());

        assertEquals("document1.doc", items.get(0).getDisplayName());
        assertEquals("Folder1", items.get(1).getDisplayName());
        assertEquals("nuxeo/workspaces/ws1/doc1", items.get(0).getSubPath());
        assertEquals("nuxeo/workspaces/ws1/doc1", items.get(0).getRelativeSubPath(""));
        assertEquals("nuxeo/workspaces/ws1/folder", items.get(1).getSubPath());
        assertEquals("nuxeo/workspaces/ws1/folder", items.get(1).getRelativeSubPath(""));
        assertEquals("workspaces/ws1/doc1", items.get(0).getRelativeSubPath("nuxeo"));
        assertEquals("workspaces/ws1/document1.doc", items.get(0).getRelativeFilePath("nuxeo"));
        assertEquals("workspaces/ws1/folder", items.get(1).getRelativeSubPath("nuxeo"));
        assertEquals("workspaces/ws1/folder", items.get(1).getRelativeFilePath("nuxeo"));

        WSSListItem item = backend.getItem("nuxeo/workspaces/ws1/doc1");
        assertNotNull(item);
        assertEquals("nuxeo/workspaces/ws1/doc1", item.getSubPath());
        assertEquals("nuxeo/workspaces/ws1/doc1", item.getRelativeSubPath(""));
        assertEquals("workspaces/ws1/doc1", item.getRelativeSubPath("nuxeo"));
        assertEquals("workspaces/ws1/document1.doc", item.getRelativeFilePath("nuxeo"));

        item = backend.getItem("nuxeo/workspaces/ws1/document1.doc");
        assertNotNull(item);
        assertEquals("nuxeo/workspaces/ws1/doc1", item.getSubPath());
        assertEquals("nuxeo/workspaces/ws1/doc1", item.getRelativeSubPath(""));
        assertEquals("workspaces/ws1/doc1", item.getRelativeSubPath("nuxeo"));
        assertEquals("workspaces/ws1/document1.doc", item.getRelativeFilePath("nuxeo"));
    }

    @Test
    public void testSimpleBackendOperations() throws Exception {
        WSSBackend backend = createWssBackend(new SimpleBackendFactory());

        WSSListItem item = backend.createFileItem("/nuxeo/workspaces/ws1", "testMe");
        assertNotNull(item);
        assertEquals("File", ((NuxeoListItem) item).getDoc().getType());
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        assertTrue(session.exists(new PathRef("/default-domain/workspaces/ws1/testMe")));

        item.checkOut("titi");
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        Lock lock = session.getLockInfo(new PathRef("/default-domain/workspaces/ws1/testMe"));
        assertNotNull(lock);
        assertEquals(lock.getOwner(), "Administrator");

        item.uncheckOut("Administrator");
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        lock = session.getLockInfo(new PathRef("/default-domain/workspaces/ws1/testMe"));
        assertNull(lock);

        item = backend.createFileItem("/nuxeo/workspaces/ws1", "testMe2");
        assertNotNull(item);
        assertEquals("File", ((NuxeoListItem) item).getDoc().getType());
        assertEquals("nuxeo/workspaces/ws1/testMe2", item.getSubPath());
        // backend.discardChanges();
        // session.save(); // for cache invalidation

        // @TODO: fix discard changes
        /*
         * assertFalse(session.exists(new PathRef( "/default-domain/workspaces/ws1/testMe2")));
         */

        item = backend.moveItem("/nuxeo/workspaces/ws1/testMe", "/nuxeo/workspaces/ws1/testMe3");
        assertNotNull(item);
        assertEquals("nuxeo/workspaces/ws1/testMe3", item.getSubPath());
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        assertTrue(session.exists(new PathRef("/default-domain/workspaces/ws1/testMe3")));

        backend.removeItem("/nuxeo/workspaces/ws1/testMe3");
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        assertFalse(session.exists(new PathRef("/default-domain/workspaces/ws1/testMe3")));
    }

    @Test
    public void testSearchBackendBrowse() throws Exception {
        WSSBackend backend = createWssBackend(new SearchBackendFactory());

        List<WSSListItem> items = backend.listItems("/nuxeo");
        assertNotNull(items);
        assertEquals(4, items.size());

        Collections.sort(items, wssListItemSorter);

        assertEquals("isolatedws", items.get(0).getName());
        assertEquals("ws1", items.get(1).getName());
        assertEquals("ws2", items.get(2).getName());
        assertEquals("ws2-1", items.get(3).getName());

        assertEquals("nuxeo/isolatedws", items.get(0).getSubPath());
        assertEquals("nuxeo/ws1", items.get(1).getSubPath());
        assertEquals("nuxeo/ws2", items.get(2).getSubPath());
        assertEquals("nuxeo/ws2-1", items.get(3).getSubPath());

        assertEquals("isolatedws", items.get(0).getRelativeSubPath("nuxeo"));
        assertEquals("ws1", items.get(1).getRelativeSubPath("nuxeo"));
        assertEquals("ws2", items.get(2).getRelativeSubPath("nuxeo"));
        assertEquals("ws2-1", items.get(3).getRelativeSubPath("nuxeo"));

        items = backend.listItems("/nuxeo/ws1");
        assertNotNull(items);
        assertEquals(2, items.size());
        Collections.sort(items, wssListItemSorter);
        assertEquals("doc1", items.get(0).getName());
        assertEquals("folder", items.get(1).getName());

        assertEquals("document1.doc", items.get(0).getDisplayName());
        assertEquals("Folder1", items.get(1).getDisplayName());
        assertEquals("nuxeo/ws1/doc1", items.get(0).getSubPath());
        assertEquals("nuxeo/ws1/doc1", items.get(0).getRelativeSubPath(""));
        assertEquals("nuxeo/ws1/folder", items.get(1).getSubPath());
        assertEquals("nuxeo/ws1/folder", items.get(1).getRelativeSubPath(""));
        assertEquals("ws1/doc1", items.get(0).getRelativeSubPath("nuxeo"));
        assertEquals("ws1/document1.doc", items.get(0).getRelativeFilePath("nuxeo"));
        assertEquals("ws1/folder", items.get(1).getRelativeSubPath("nuxeo"));
        assertEquals("ws1/folder", items.get(1).getRelativeFilePath("nuxeo"));

        WSSListItem item = backend.getItem("nuxeo/ws1/doc1");
        assertNotNull(item);
        assertEquals("nuxeo/ws1/doc1", item.getSubPath());
        assertEquals("nuxeo/ws1/doc1", item.getRelativeSubPath(""));
        assertEquals("ws1/doc1", item.getRelativeSubPath("nuxeo"));
        assertEquals("ws1/document1.doc", item.getRelativeFilePath("nuxeo"));

        item = backend.getItem("nuxeo/ws1");
        assertNotNull(item);
        assertEquals("nuxeo/ws1", item.getSubPath());
        assertEquals("nuxeo/ws1", item.getRelativeSubPath(""));
        assertEquals("ws1", item.getRelativeSubPath("nuxeo"));
        assertEquals("ws1", item.getRelativeFilePath("nuxeo"));

        item = backend.getItem("nuxeo/ws2-1/doc2");
        assertNotNull(item);
        assertEquals("nuxeo/ws2-1/doc2", item.getSubPath());
        assertEquals("nuxeo/ws2-1/doc2", item.getRelativeSubPath(""));
        assertEquals("ws2-1/doc2", item.getRelativeSubPath("nuxeo"));
        assertEquals("ws2-1/document2.doc", item.getRelativeFilePath("nuxeo"));

        item = backend.getItem("nuxeo/ws2-1");
        assertNotNull(item);
        assertEquals("nuxeo/ws2-1", item.getSubPath());
        assertEquals("nuxeo/ws2-1", item.getRelativeSubPath(""));
        assertEquals("ws2-1", item.getRelativeSubPath("nuxeo"));
        assertEquals("ws2-1", item.getRelativeFilePath("nuxeo"));
        assertEquals("Ws2", item.getDisplayName());
    }

    @Test
    public void testSearchBackendOperations() throws Exception {
        WSSBackend backend = createWssBackend(new SearchBackendFactory());

        WSSListItem item = backend.createFileItem("/nuxeo/ws1", "testMe");
        assertNotNull(item);
        assertEquals("File", ((NuxeoListItem) item).getDoc().getType());
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        assertNotNull("Item must exists", backend.getItem("/nuxeo/ws1/testMe"));
        assertTrue("Document must exists", session.exists(new PathRef("/default-domain/workspaces/ws1/testMe")));

        item.checkOut("titi");
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        Lock lock = session.getLockInfo(new PathRef("/default-domain/workspaces/ws1/testMe"));
        assertNotNull(lock);
        assertEquals(lock.getOwner(), "Administrator");

        item.uncheckOut("Administrator");
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        lock = session.getLockInfo(new PathRef("/default-domain/workspaces/ws1/testMe"));
        assertNull(lock);

        item = backend.createFileItem("/nuxeo/ws1", "testMe2");
        assertNotNull(item);
        assertEquals("File", ((NuxeoListItem) item).getDoc().getType());
        assertEquals("nuxeo/ws1/testMe2", item.getSubPath());
        // backend.discardChanges();
        // session.save(); // for cache invalidation

        // @TODO: fix discard changes
        // assertFalse(session.exists(new
        // PathRef("/default-domain/workspaces/ws1/testMe2")));

        item = backend.moveItem("/nuxeo/ws1/testMe", "/nuxeo/ws1/testMe3");
        assertNotNull(item);
        assertEquals("nuxeo/ws1/testMe3", item.getSubPath());
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        assertTrue(session.exists(new PathRef("/default-domain/workspaces/ws1/testMe3")));

        // move across
        item = backend.moveItem("/nuxeo/ws1/testMe3", "/nuxeo/ws2/testMe3");
        assertNotNull(item);
        assertEquals("nuxeo/ws2/testMe3", item.getSubPath());
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        assertTrue(session.exists(new PathRef("/default-domain/workspaces/ws1/folder/ws2/testMe3")));

        backend.removeItem("/nuxeo/ws2/testMe3");
        backend.saveChanges(); // for cache invalidation
        // session.save(); // for cache invalidation
        assertFalse(session.exists(new PathRef("/default-domain/workspaces/ws1/folder/ws2/testMe3")));
    }
}
