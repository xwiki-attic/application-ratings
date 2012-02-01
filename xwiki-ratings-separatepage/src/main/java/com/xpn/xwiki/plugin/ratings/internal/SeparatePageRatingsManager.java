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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.ratings.Rating;
import com.xpn.xwiki.plugin.ratings.RatingsException;
import com.xpn.xwiki.plugin.ratings.RatingsManager;

/**
 * @version $Id$
 * @see RatingsManager
 * @see AbstractRatingsManager
 */
@Component
@Named("separatepage")
public class SeparatePageRatingsManager extends AbstractRatingsManager
{
    @Inject
    private Logger logger;

    public String getRatingsSpaceName()
    {
        XWikiContext context = getXWikiContext();

        String ratingsSpaceName = context.getWiki().Param("xwiki.ratings.separatepagemanager.spacename", "");
        ratingsSpaceName =
            context.getWiki().getXWikiPreference("ratings_separatepagemanager_spacename", ratingsSpaceName, context);
        return ratingsSpaceName;
    }

    public boolean hasRatingsSpaceForeachSpace()
    {
        XWikiContext context = getXWikiContext();

        int result = (int) context.getWiki().ParamAsLong("xwiki.ratings.separatepagemanager.hasratingsforeachspace", 0);
        return (context.getWiki().getXWikiPreferenceAsInt("ratings_separatepagemanager_hasratingsforeachspace", result,
            context) == 1);
    }

    @Override
    public Rating setRating(String documentName, String author, int vote) throws RatingsException
    {
        XWikiContext context = getXWikiContext();

        Rating rating = getRating(documentName, author);
        int oldVote;
        if (rating == null) {
            oldVote = 0;
            rating = new SeparatePageRating(documentName, author, vote, this, context);
        } else {
            oldVote = rating.getVote();
            rating.setVote(vote);
            rating.setDate(new Date());
        }
        rating.save();
        // update the average rating
        updateAverageRatings(documentName, rating, oldVote);

        // update reputation
        updateReputation(documentName, rating, oldVote);
        return rating;
    }

    @Override
    public List<Rating> getRatings(String documentName, int start, int count, boolean asc) throws RatingsException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Calling separate page manager code for ratings");
        }

        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        List<Rating> ratings = new ArrayList<Rating>();

        // FIXME: use start and count in the query.
        String sql =
            ", BaseObject as obj, StringProperty as parentprop where doc.fullName=obj.name and obj.className='"
                + getRatingsClassName()
                + "' and obj.id=parentprop.id.id and parentprop.id.name='"
                + RATING_CLASS_FIELDNAME_PARENT
                + "' and parentprop.value='"
                + documentName
                + "' and obj.name not in (select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className='"
                + getRatingsClassName()
                + "' and obj2.id=statusprop.id.id and statusprop.id.name='status' and (statusprop.value='moderated' or statusprop.value='refused') and obj.id=obj2.id) order by doc.date "
                + (asc ? "asc" : "desc");

        try {
            List<String> ratingPageNameList = xwiki.getStore().searchDocumentsNames(sql, count, start, context);

            for (String ratingPageName : ratingPageNameList) {
                ratings.add(getRatingFromDocument(documentName, xwiki.getDocument(ratingPageName, context), context));
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }

        return ratings;
    }

    @Override
    public Rating getRating(String documentName, int id) throws RatingsException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        String sql =
            ", BaseObject as obj, StringProperty as parentprop where doc.fullName=obj.name and obj.className='"
                + getRatingsClassName()
                + "' and obj.id=parentprop.id.id and parentprop.id.name='"
                + RATING_CLASS_FIELDNAME_PARENT
                + "' and parentprop.value='"
                + documentName
                + "' and obj.name not in (select obj2.name from BaseObject as obj2, StringProperty as statusprop where obj2.className='"
                + getRatingsClassName()
                + "' and obj2.id=statusprop.id.id and statusprop.id.name='status' and (statusprop.value='moderated' or statusprop.value='refused') and obj.id=obj2.id) order by doc.date desc";
        try {
            List<String> ratingPageNameList = xwiki.getStore().searchDocumentsNames(sql, 1, id, context);
            if ((ratingPageNameList == null) || (ratingPageNameList.size() == 0)) {
                return null;
            } else {
                return new SeparatePageRatingsManager().getRatingFromDocument(documentName,
                    xwiki.getDocument(ratingPageNameList.get(0), context), context);
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public Rating getRating(String ratingId) throws RatingsException
    {
        XWikiContext context = getXWikiContext();

        try {
            int i1 = ratingId.indexOf(".");
            if (i1 == -1) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, cannot parse rating id");
            }

            XWikiDocument doc = context.getWiki().getDocument(ratingId, context);
            if (doc.isNew()) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, rating does not exist");
            }

            BaseObject object = doc.getObject(getRatingsClassName());
            if (object == null) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, rating does not exist");
            }

            String parentDocName = object.getStringValue(RATING_CLASS_FIELDNAME_PARENT);

            return new SeparatePageRating(parentDocName, doc, this, context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    public Rating getRatingFromDocument(String documentName, XWikiDocument doc, XWikiContext context)
        throws RatingsException
    {
        return new SeparatePageRating(documentName, doc, this, context);
    }
}
