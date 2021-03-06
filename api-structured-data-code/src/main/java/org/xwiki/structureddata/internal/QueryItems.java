/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xwiki.structureddata.internal;

import java.util.Map;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * Get the query returning the list of items for a given Application.
 *
 * @version $Id$
 */
public class QueryItems {
    protected static Query getQuery(XWikiContext context, QueryManager queryManager, String xClassFullName, Map<String, Object> options, String appWhereClause, String appSelectClause) throws QueryException, XWikiException {
        String queryOpt = "query";
        String hiddenOpt = "hidden";
        String limitOpt = "limit";
        String offsetOpt = "offset";
        String orderOpt = "order";

        // Main search query
        String queryString = "select " + appSelectClause
                + " from Document doc, doc.object( '" + xClassFullName + "' ) as item ";

        // If a "query" is passed in the options, it should be used to complete the query, and the others options
        // should be ignored. If there is no "query" options, we should use the standard query structure with the
        // selected options.
        if (options.containsKey(queryOpt)) {
            String whereClause = options.get(queryOpt).toString().trim();
            if(whereClause.substring(0,6).toLowerCase().equals("where ")) {
                whereClause = whereClause.substring(6).trim();
            }
            queryString += "where " + whereClause;
        }
        else {
            // Create a filter to remove class templates from the results
            // /!\ Templates can be named ApplicationClassTemplate or ApplicationTemplate
            String templateFilter = " and item.name <> '" + xClassFullName + "Template' ";
            if (xClassFullName.length() > 5 && xClassFullName.endsWith("Class")) {
                String shortClassName = xClassFullName.substring(0, xClassFullName.length() - 5);
                templateFilter += " and item.name <> '" + shortClassName + "Template' ";
            }
            // Add the application filter (i.e. "Data" space for AWM app) and the template filters
            queryString += "where " + appWhereClause + templateFilter;
            // Hide the hidden documents except if it is explicitly requested to display them or if the user has
            // chosen to display them in his profile
            Boolean viewHidden = getViewHiddenDocuments(context); // Get the value in the user's profile
            if(!viewHidden) {
                if (options.containsKey(hiddenOpt)) {
                    String hiddenValue = options.get(hiddenOpt).toString();
                    if (!(hiddenValue.equals("true") || hiddenValue.equals("1"))) {
                        queryString += " and (doc.hidden <> true or doc.hidden is null)";
                    }
                } else {
                    queryString += " and (doc.hidden <> true or doc.hidden is null)";
                }
            }
            // Order the results by the name of the document (and object number if applicable) or by the specified
            // property
            if (options.containsKey(orderOpt)) {
                queryString += " order by " + options.get(orderOpt);
            } else {
                queryString += " order by " + appSelectClause;
            }
        }
        // Execute the query
        Query query = queryManager.createQuery(queryString, Query.XWQL);
        // Filter the results depending on optional parameters
        if (options.containsKey(limitOpt)) {
            query = query.setLimit((Integer) options.get(limitOpt));
        }
        if (options.containsKey(offsetOpt)) {
            query = query.setOffset((Integer) options.get(offsetOpt));
        }

        return query;
    }

    protected static Boolean getViewHiddenDocuments(XWikiContext context) {
        try {
            DocumentReference userRef = context.getUserReference();
            XWiki xwiki = context.getWiki();
            XWikiDocument userDoc = xwiki.getDocument(userRef, context);
            ObjectReference objRef = new ObjectReference("XWiki.XWikiUsers", userRef);
            ObjectPropertyReference objPropRef = new ObjectPropertyReference("displayHiddenDocuments", objRef);
            BaseProperty hiddenDocProp = userDoc.getXObjectProperty(objPropRef);
            Object value = hiddenDocProp.getValue();
            return value != null && Integer.parseInt(value.toString()) == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
