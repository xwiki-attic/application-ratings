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
package com.xpn.xwiki.plugin.ratings.internal;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.plugin.ratings.Rating;
import com.xpn.xwiki.plugin.ratings.RatingsException;
import com.xpn.xwiki.plugin.ratings.RatingsManager;
import com.xpn.xwiki.plugin.ratings.RatingsPlugin;

/**
 * @version $Id: $
 * @see Rating
 */
public class SeparatePageRating implements Rating
{
    private String documentName;

    private XWikiDocument document;

    private XWikiContext context;

    public SeparatePageRating(String documentName, String author, int vote, XWikiContext context)
        throws RatingsException
    {
        this(documentName, author, new Date(), vote, context);
    }

    public SeparatePageRating(String documentName, String author, Date date, int vote, XWikiContext context)
        throws RatingsException
    {
        this.documentName = documentName;
        this.context = context;
        this.document = addDocument(documentName, author, date, vote);
    }

    public SeparatePageRating(String documentName, XWikiDocument doc, XWikiContext context) throws RatingsException
    {
        this.documentName = documentName;
        this.context = context;
        this.document = doc;
    }

    public SeparatePageRatingsManager getRatingsManager()
    {
        return (SeparatePageRatingsManager) ((RatingsPlugin) context.getWiki().getPlugin(
            RatingsPlugin.RATINGS_PLUGIN_NAME, context)).getRatingsManager(context);
    }

    /**
     * RatingId represent the ID of the rating In this case it is the page name
     */
    public String getRatingId()
    {
        return getDocument().getFullName();
    }

    public String getGlobalRatingId()
    {
        return getRatingId();
    }

    public BaseObject getAsObject()
    {
        return getDocument().getObject(getRatingsManager().getRatingsClassName(context));
    }

    public XWikiDocument getDocument()
    {
        if (document == null) {
            try {
                document = context.getWiki().getDocument(getPageName(this.documentName, context), context);
            } catch (XWikiException e) {
                return null;
            }
        }
        return document;
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#getAuthor()
     */
    public String getAuthor()
    {
        return getAsObject().getStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR);
    }

    public Date getDate()
    {

        return getAsObject().getDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#setAuthor(String)
     */
    public void setAuthor(String author)
    {
        getAsObject().setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, author);
    }

    public void setDate(Date date)
    {
        getAsObject().setDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE, date);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#getVote()
     */
    public int getVote()
    {
        return getAsObject().getIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#setVote(int)
     */
    public void setVote(int vote)
    {
        getAsObject().setIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, vote);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#get(String)
     */
    public Object get(String propertyName)
    {
        try {
            return ((BaseProperty) getAsObject().get(propertyName)).getValue();
        } catch (XWikiException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#display(String,String,XWikiContext)
     */
    public String display(String propertyName, String mode, XWikiContext context)
    {
        return document.display(propertyName, mode, getAsObject(), context);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#save()
     */
    public void save() throws RatingsException
    {
        try {
            if (document == null) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_SAVERATING_NULLDOCUMENT,
                    "Cannot save invalid separate page rating, the rating document is null");
            }
            // Force content dirty to false, so that the content update date is not changed when saving the document.
            // This should not be handled there, since it is not the responsibility of this plugin to decide if
            // the content has actually been changed or not since current revision, but the implementation of
            // this in XWiki core is wrong. See http://jira.xwiki.org/jira/XWIKI-2800 for more details.
            // There is a draw-back to doing this, being that if the document content is being changed before
            // the document is rated, the contentUpdateDate will not be modified. Although possible, this is very
            // unlikely to happen, or to be a use case. The default rating application will use an asynchronous service
            // to
            // note a document, which service will only set the rating, so the behavior will be correct.
            document.setContentDirty(false);
            context.getWiki().saveDocument(getDocument(), context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#remove()
     */
    public boolean remove() throws RatingsException
    {
        try {
            XWikiDocument doc = getDocument();
            // remove the rating
            context.getWiki().deleteDocument(doc, context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
        return true;
    }

    /**
     * Generate page name from the container page We add Rating and getUniquePageName will add us a counter to our page
     */
    private String getPageName(String documentName, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = context.getWiki().getDocument(documentName, context);
        String ratingsSpace = getRatingsManager().getRatingsSpaceName(context);
        boolean hasRatingsSpaceForeachSpace = getRatingsManager().hasRatingsSpaceForeachSpace(context);
        if (hasRatingsSpaceForeachSpace) {
            return doc.getSpace() + ratingsSpace + "."
                + getUniquePageName(ratingsSpace, doc.getName(), "R", true, context);
        } else if (ratingsSpace == null) {
            return doc.getSpace() + "." + getUniquePageName(doc.getSpace(), doc.getName() + "R", "", true, context);
        } else {
            return ratingsSpace + "."
                + getUniquePageName(ratingsSpace, doc.getSpace() + "_" + doc.getName(), "R", true, context);
        }
    }

    private String getUniquePageName(String space, String name, String postfix, boolean forcepostfix,
        XWikiContext context)
    {
        String pageName = context.getWiki().clearName(name, context);
        if (forcepostfix || context.getWiki().exists(space + "." + pageName, context)) {
            int i = 1;
            while (context.getWiki().exists(space + "." + pageName + postfix + i, context)) {
                i++;
            }
            return pageName + postfix + i;
        }
        return pageName;
    }

    private XWikiDocument addDocument(String documentName, String author, Date date, int vote) throws RatingsException
    {
        try {
            String ratingsClassName = getRatingsManager().getRatingsClassName(context);
            String pageName = getPageName(documentName, context);
            String parentDocName = documentName;
            com.xpn.xwiki.XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument(pageName, context);
            doc.setParent(parentDocName);
            BaseObject obj = new BaseObject();
            obj.setClassName(ratingsClassName);
            obj.setName(pageName);
            obj.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, author);
            obj.setDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE, date);
            obj.setIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, vote);
            obj.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_PARENT, parentDocName);
            doc.addObject(ratingsClassName, obj);
            doc.setContent("#includeForm(\"XWiki.XWikiRatingSheet\")");
            return doc;
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    public String toString()
    {
        boolean shouldAddSpace = false;
        StringBuffer sb = new StringBuffer();
        if (getAuthor() != null) {
            sb.append("\nAuthor=").append(getAuthor());
            shouldAddSpace = true;
        }
        if (getDate() != null) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("\nDate=").append(getDate());
            shouldAddSpace = true;
        }
        if (getVote() != 0) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("\nVote=").append(getVote()).append("\n");
            shouldAddSpace = true;
        }

        return sb.toString();
    }

    public String getDocumentName()
    {
        return this.documentName;
    }
}
