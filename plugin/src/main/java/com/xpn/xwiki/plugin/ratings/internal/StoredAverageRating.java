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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.ratings.AverageRating;
import com.xpn.xwiki.plugin.ratings.RatingsException;
import com.xpn.xwiki.plugin.ratings.RatingsManager;

/**
 * @version $Id$
 * @see AverageRating
 */
public class StoredAverageRating implements AverageRating
{
    private XWikiDocument document;

    private BaseObject object;

    public StoredAverageRating(XWikiDocument document, BaseObject ratingObject)
    {
        this.document = document;
        this.object = ratingObject;
    }

    public int getNbVotes()
    {
        return object.getIntValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_NBVOTES);
    }

    public void setNbVotes(int nbVotes)
    {
        object.setIntValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_NBVOTES, nbVotes);
    }

    public float getAverageVote()
    {
        return object.getFloatValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE);
    }

    public void setAverageVote(float averageVote)
    {
        object.setFloatValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE, averageVote);
    }

    public String getMethod()
    {
        return object.getStringValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD);
    }

    public void setMethod(String method)
    {
        object.setStringValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD, method);
    }

    /**
     * FIXME: move all this save logic from the ratings to the manager. Move towards fly-weight ratings.
     */
    public void save(XWikiContext context) throws RatingsException
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
            document.setContentDirty(false);

            String saveMessage = "Computed average rating";

            context.getWiki().saveDocument(document, saveMessage, true, context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }
}
