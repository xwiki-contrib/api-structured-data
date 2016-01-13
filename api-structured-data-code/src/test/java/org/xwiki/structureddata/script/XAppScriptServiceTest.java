/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.structureddata.script;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.structureddata.internal.ItemMap;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Tests for the Extensible API for structured data.
 */
public class XAppScriptServiceTest
{
   
    @Rule
    public final MockitoComponentMockingRule<XAppScriptService> mocker = new MockitoComponentMockingRule<>(XAppScriptService.class);
    
    /**
     * The object being tested.
     */
    private XAppScriptService xApp;

    private XWikiContext xcontext;
    private XWikiDocument myclassdoc;
    private BaseClass myclass;
    private DocumentReference classRef;

    @Before
    public void setUp() throws Exception
    {
        xApp = mocker.getComponentUnderTest();
        xcontext = mock(XWikiContext.class);
        Provider<XWikiContext> xcontextProvider = mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(xcontext);

        XWiki wiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(wiki);

        // Create the class in the app with two properties (prop1=String, prop2=StaticList)
        myclassdoc = mock(XWikiDocument.class);
        myclass = mock(BaseClass.class);
        classRef = new DocumentReference("wiki", "My", "Class");
        when(xcontext.getWiki().getXClass(classRef, xcontext)).thenReturn(myclass);
        when(xcontext.getWiki().getDocument(classRef, xcontext)).thenReturn(myclassdoc);

        PropertyClass prop1 = mock(PropertyClass.class);
        StaticListClass prop2 = mock(StaticListClass.class);
        List<PropertyClass> propList = new ArrayList<>();
        propList.add(prop1);
        propList.add(prop2);

        when(myclass.getEnabledProperties()).thenReturn(propList);
        when(prop1.getName()).thenReturn("prop1");
        when(prop2.getName()).thenReturn("prop2");
        when(prop1.getClassType()).thenReturn("String");
        when(prop2.getClassType()).thenReturn("StaticList");
        when(prop2.getValues()).thenReturn("Paris|Iasi");
    }

    @Test
    public void testGetAppSchema() throws Exception
    {
        // Create expected schema
        Map<String, Object> schema = new HashMap<>();
        Map<String, Object> prop1schema = new HashMap<>();
        Map<String, Object> prop2schema = new HashMap<>();
        prop1schema.put("Type", "String");
        schema.put("prop1", prop1schema);
        prop2schema.put("Type", "StaticList");
        prop2schema.put("Values", "Paris|Iasi");
        schema.put("prop2", prop2schema);
        
        // CHeck the result
        Assert.assertEquals(xApp.getApp(classRef).getAppSchema(), schema);
    }

    @Test
    public void testGetItems() throws Exception
    {
        QueryManager qm = xApp.queryManager;
        DocumentReferenceResolver<String> resolver = xApp.resolver; 

        // Create 2 items
        String objDocName1 = "MyClassData.Item1";
        DocumentReference objDocRef1 = new DocumentReference("xwiki", "MyClassData", "Item1");
        ItemMap item1Map = this.createObject(resolver, objDocName1, objDocRef1, "ValueString1", "Paris");
        String objDocName2 = "MyClassData.Item2";
        DocumentReference objDocRef2 = new DocumentReference("xwiki", "MyClassData", "Item2");
        ItemMap item2Map = this.createObject(resolver, objDocName2, objDocRef2, "ValueString1", "Paris");
        
        // Create the xwql query result
        Query query = mock(Query.class);
        when(qm.createQuery((String) any(), (String) any())).thenReturn(query);
        Object[] queryObj1 = {objDocName1, 0};
        Object[] queryObj2 = {objDocName2, 0};
        List<Object[]> queryList = new ArrayList<>();
        queryList.add(queryObj1);
        queryList.add(queryObj2);
        doReturn(queryList).when(query).execute();
        
        // Create the expected result
        Map<String, Object> result = new HashMap<>();
        result.put("Object0", item1Map);
        result.put("Object1", item2Map);
        
        // Check the result
        Assert.assertEquals(xApp.getApp(classRef).getItems(), result);
    }

    @Test
    public void testGetItem() throws Exception
    {
        DocumentReferenceResolver<String> resolver = xApp.resolver;
        
        // Create an item 
        DocumentReference objDocRef = new DocumentReference("xwiki", "MyClassData", "Item1");
        String objDocName = "MyClassData.Item1";
        ItemMap itemMap = this.createObject(resolver, objDocName, objDocRef, "ValueString1", "Paris");
        
        // Check the result
        Assert.assertEquals(xApp.getApp(classRef).getItem(objDocName), itemMap);
    }

    @Test
    public void testStoreItem() throws XWikiException, Exception
    {
        DocumentReferenceResolver<String> resolver = xApp.resolver;
        
        // Create the initial object
        DocumentReference objDocRef = new DocumentReference("xwiki", "MyClassData", "Item");
        String objDocName = "MyClassData.Item";
        ItemMap itemMap = this.createObject(resolver, objDocName, objDocRef, "ValueString", "Paris");
        XWikiDocument doc = xcontext.getWiki().getDocument(objDocRef, xcontext);
        BaseObject obj = doc.getXObject(classRef, 0);
        
        // Create the new data
        ItemMap dataMap = (ItemMap) itemMap.get("data");
        dataMap.put("prop1", "NewValueString");
        
        // Create the result map
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("Success", "1");
        
        // Check for success      
        Assert.assertEquals(xApp.getApp(classRef).storeItem(objDocName, dataMap), resultMap);
        // Check that properties have been updated and modifications have been saved
        verify(obj).set("prop1", "NewValueString", xcontext);
        verify(obj).set("prop2", "Paris", xcontext);
        verify(xcontext.getWiki()).saveDocument(doc, "Properties updated", xcontext);
    }

    @Test
    public void testDeleteItem() throws XWikiException, Exception
    {
        DocumentReferenceResolver<String> resolver = xApp.resolver;
        
        // Create the initial object
        DocumentReference objDocRef = new DocumentReference("xwiki", "MyClassData", "Item");
        String objDocName = "MyClassData.Item";
        this.createObject(resolver, objDocName, objDocRef, "ValueString", "Paris");
        XWikiDocument doc = xcontext.getWiki().getDocument(objDocRef, xcontext);
        BaseObject obj = doc.getXObject(classRef, 0);
        
        // Create the result map
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("Success", "1");
        
        // Check for success      
        Assert.assertEquals(xApp.getApp(classRef).deleteItem(objDocName), resultMap);
        // Check that that object has been deleted and the document has been saved
        verify(doc).removeXObject(obj);
        verify(xcontext.getWiki()).saveDocument(doc, xcontext);
    }

    private ItemMap createObject(DocumentReferenceResolver<String> resolver, String docName, DocumentReference docRef, String valueProp1, String valueProp2) throws XWikiException
    {
        XWikiDocument objDoc = mock(XWikiDocument.class);
        String objDocName = docName;
        BaseObject obj = mock(BaseObject.class);
        when(resolver.resolve(objDocName)).thenReturn(docRef);
        when(xcontext.getWiki().getDocument(docRef, xcontext)).thenReturn(objDoc);
        when(objDoc.getXObject(classRef, 0)).thenReturn(obj);
        StringProperty prop1Obj = mock(StringProperty.class);
        when(obj.getField("prop1")).thenReturn(prop1Obj);
        when(prop1Obj.getValue()).thenReturn(valueProp1);
        StringListProperty prop2Obj = mock(StringListProperty.class);
        when(obj.getField("prop2")).thenReturn(prop2Obj);
        when(prop2Obj.getValue()).thenReturn(valueProp2);
        
        ItemMap result = new ItemMap();
        ItemMap prop1data = new ItemMap();
        result.put("id", docName);
        prop1data.put("prop1", valueProp1);
        prop1data.put("prop2", valueProp2);
        result.put("data", prop1data);
        return result;
    }
}
