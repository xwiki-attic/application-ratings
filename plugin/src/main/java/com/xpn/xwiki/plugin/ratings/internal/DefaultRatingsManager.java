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

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.ratings.Rating;
import com.xpn.xwiki.plugin.ratings.RatingsException;

/**
 * @version $Id$
 */
@Component
public class DefaultRatingsManager extends AbstractRatingsManager
{
    @Override
    public Rating setRating(String documentName, String author, int vote) throws RatingsException
    {
        XWikiContext context = getXWikiContext();

        Rating rating = getRating(documentName, author);
        int oldVote;
        if (rating == null) {
            oldVote = 0;
            rating = new DefaultRating(documentName, author, vote, this, context);
        } else {
            oldVote = rating.getVote();
            rating.setVote(vote);
            rating.setDate(new Date());
        }
        rating.save();
        updateAverageRatings(documentName, rating, oldVote);
        return rating;
    }

    @Override
    public List<Rating> getRatings(String documentName, int start, int count, boolean asc) throws RatingsException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Calling default manager code for ratings");
        }

        XWikiContext context = getXWikiContext();

        List<Rating> ratings = new ArrayList<Rating>();

        try {
            int skipped = 0;
            int nb = 0;
            XWikiDocument doc = context.getWiki().getDocument(documentName, context);
            List<BaseObject> bobjects = doc.getObjects(getRatingsClassName());
            if (bobjects != null) {

                for (BaseObject bobj : bobjects) {
                    if (bobj != null) {
                        if (skipped < start) {
                            skipped++;
                        } else {
                            ratings.add(getDefaultRating(documentName, bobj));
                            nb++;
                        }
                        if ((count != 0) && (nb == count)) {
                            break;
                        }
                    }
                }
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }

        return ratings;
    }

    @Override
    public Rating getRating(String ratingId) throws RatingsException
    {
        XWikiContext context = getXWikiContext();

        try {
            int i1 = ratingId.indexOf(":");
            if (i1 == -1) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, cannot parse rating id");
            }

            String docName = ratingId.substring(0, i1);
            String sObjectNb = ratingId.substring(i1 + 1);
            int objectNb = Integer.parseInt(sObjectNb);
            XWikiDocument doc = context.getWiki().getDocument(docName, context);

            if (doc.isNew()) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, document does not exist");
            }

            BaseObject object = doc.getObject(getRatingsClassName(), objectNb);

            if (object == null) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, could not find rating");
            }

            return new DefaultRating(docName, object, this, context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public Rating getRating(String documentName, int id) throws RatingsException
    {
        XWikiContext context = getXWikiContext();

        try {
            int skipped = 0;
            XWikiDocument doc = context.getWiki().getDocument(documentName, context);
            List<BaseObject> bobjects = doc.getObjects(getRatingsClassName());
            if (bobjects != null) {
                for (BaseObject bobj : bobjects) {
                    if (bobj != null) {
                        if (skipped < id) {
                            skipped++;
                        } else {
                            return getDefaultRating(documentName, bobj);
                        }
                    }
                }
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
        return null;
    }

    private DefaultRating getDefaultRating(String documentName, BaseObject bobj)
    {
        XWikiContext context = getXWikiContext();
        return new DefaultRating(documentName, bobj, this, context);
    }
}
