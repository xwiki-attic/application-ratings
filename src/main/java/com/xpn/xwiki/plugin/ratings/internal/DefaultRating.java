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
 * @see Rating
 */
public class DefaultRating implements Rating
{
    private String documentName;

    private XWikiDocument document;

    private BaseObject object;

    private XWikiContext context;

    public DefaultRating(String documentName, String author, int vote, XWikiContext context)
    {
        this(documentName, author, new Date(), vote, context);
    }

    public DefaultRating(String documentName, String author, Date date, int vote, XWikiContext context)
    {
        this.context = context;
        this.documentName = documentName;

        createObject(documentName, author, date, vote);
    }

    public DefaultRating(String documentName, BaseObject obj, XWikiContext context)
    {
        this.document = getDocument();
        this.object = obj;
        this.context = context;
    }

    public RatingsManager getRatingsManager()
    {
        return ((RatingsPlugin) context.getWiki().getPlugin(RatingsPlugin.RATINGS_PLUGIN_NAME, context))
            .getRatingsManager(context);
    }

    /**
     * RatingId represente the rating ID. It is the object number in the default ratings case
     */
    public String getRatingId()
    {
        return "" + object.getNumber();
    }

    /**
     * RatingId represente the rating ID. It is the object number in the default ratings case
     */
    public String getGlobalRatingId()
    {
        return document.getFullName() + ":" + object.getNumber();
    }

    public BaseObject getAsObject()
    {
        return object;
    }

    public XWikiDocument getDocument()
    {
        if (document == null) {
            try {
                document = context.getWiki().getDocument(this.documentName, context);
            } catch (XWikiException e) {
                // do nothing
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
        return object.getStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR);
    }

    public void setAuthor(String author)
    {
        object.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, author);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#getDate()
     */
    public Date getDate()
    {
        return object.getDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE);
    }

    public void setDate(Date date)
    {
        object.setDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE, date);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#getVote()
     */
    public int getVote()
    {
        return object.getIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE);
    }

    public void setVote(int vote)
    {
        object.setIntValue("vote", vote);
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
     * @see com.xpn.xwiki.plugin.ratings.Rating#display(String,String, com.xpn.xwiki.XWikiContext)
     */
    public String display(String propertyName, String mode, XWikiContext context)
    {
        return document.display(propertyName, mode, object, context);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#getDocumentName()
     */
    public String getDocumentName()
    {
        return this.documentName;
    }

    public void save() throws RatingsException
    {
        try {
            // Force content dirty to false, so that the content update date is not changed when saving the document.
            // This should not be handled there, since it is not the responsibility of this plugin to decide if
            // the content has actually been changed or not since current revision, but the implementation of
            // this in XWiki core is wrong. See http://jira.xwiki.org/jira/XWIKI-2800 for more details.
            // There is a draw-back to doing this, being that if the document content is being changed before
            // the document is rated, the contentUpdateDate will not be modified. Although possible, this is very
            // unlikely to happen, or to be a use case. The default rating application will use an asynchronous service
            // to
            // note a document, which service will only set the rating, so the behavior will be correct.
            getDocument().setContentDirty(false);
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
    public boolean remove()
    {
        return remove(true);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.ratings.Rating#remove()
     */
    protected boolean remove(boolean withSave)
    {
        try {
            XWikiDocument doc = getDocument();
            if (!doc.removeObject(object)) {
                return false;
            } else {
                // save is needed to remove effectively
                if (withSave) {
                    context.getWiki().saveDocument(doc, context);
                }
                return true;
            }
        } catch (XWikiException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void createObject(String documentName, String author, Date date, int vote)
    {
        XWikiDocument doc = getDocument();

        String ratingsClassName = getRatingsManager().getRatingsClassName(context);
        BaseObject obj = new BaseObject();
        obj.setClassName(ratingsClassName);
        obj.setName(doc.getFullName());
        // read data from map
        obj.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, author);
        obj.setDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE, date);
        obj.setIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, vote);
        obj.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_PARENT, documentName);
        doc.addObject(ratingsClassName, obj);
        // set the internal variable
        object = obj;
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
        if (getVote() != -1) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("\nVote=").append(getVote()).append("\n");
            shouldAddSpace = true;
        }

        return sb.toString();
    }
}
