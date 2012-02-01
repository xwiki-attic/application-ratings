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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.ratings.AverageRatingApi;
import com.xpn.xwiki.plugin.ratings.Rating;
import com.xpn.xwiki.plugin.ratings.RatingApi;
import com.xpn.xwiki.plugin.ratings.RatingsComponent;

/**
 * @version $Id$
 */
@Component
@Named("ratings")
@Singleton
public class RatingsScriptService implements ScriptService
{
    @Inject
    private RatingsComponent ratingsComponent;

    @Inject
    private Execution execution;

    protected RatingsComponent getRatingsComponent()
    {
        return ratingsComponent;
    }

    protected List<RatingApi> wrapRatings(List<Rating> ratings)
    {
        if (ratings == null) {
            return null;
        }

        XWikiContext context = getXWikiContext();

        List<RatingApi> ratingsResult = new ArrayList<RatingApi>();
        for (Rating rating : ratings) {
            ratingsResult.add(new RatingApi(rating, context));
        }
        return ratingsResult;
    }

    public RatingApi setRating(Document doc, String author, int vote)
    {
        XWikiContext context = getXWikiContext();

        // TODO protect this with programming rights
        // and add a setRating(docName), not protected but for which the author is retrieved from context.
        try {
            return new RatingApi(getRatingsComponent().setRating(doc.getFullName(), author, vote), context);
        } catch (Throwable e) {
            context.put("exception", e);
            return null;
        }
    }

    public RatingApi getRating(Document doc, String author)
    {
        XWikiContext context = getXWikiContext();

        try {
            Rating rating = getRatingsComponent().getRating(doc.getFullName(), author);
            if (rating == null) {
                return null;
            }
            return new RatingApi(rating, context);
        } catch (Throwable e) {
            context.put("exception", e);
            return null;
        }
    }

    public List<RatingApi> getRatings(Document doc, int start, int count)
    {
        return getRatings(doc, start, count, true);
    }

    public List<RatingApi> getRatings(Document doc, int start, int count, boolean asc)
    {
        XWikiContext context = getXWikiContext();

        try {
            return wrapRatings(getRatingsComponent().getRatings(doc.getFullName(), start, count, asc));
        } catch (Exception e) {
            context.put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getAverageRating(Document doc, String method)
    {
        XWikiContext context = getXWikiContext();

        try {
            return new AverageRatingApi(getRatingsComponent().getAverageRating(doc.getFullName(), method), context);
        } catch (Throwable e) {
            context.put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getAverageRating(Document doc)
    {
        XWikiContext context = getXWikiContext();

        try {
            return new AverageRatingApi(getRatingsComponent().getAverageRating(doc.getFullName()), context);
        } catch (Throwable e) {
            context.put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getAverageRating(String fromsql, String wheresql, String method)
    {
        XWikiContext context = getXWikiContext();

        try {
            return new AverageRatingApi(getRatingsComponent().getAverageRating(fromsql, wheresql, method), context);
        } catch (Throwable e) {
            context.put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getAverageRating(String fromsql, String wheresql)
    {
        XWikiContext context = getXWikiContext();

        try {
            return new AverageRatingApi(getRatingsComponent().getAverageRatingFromQuery(fromsql, wheresql), context);
        } catch (Throwable e) {
            context.put("exception", e);
            return null;
        }
    }

    public AverageRatingApi getUserReputation(String username)
    {
        XWikiContext context = getXWikiContext();

        try {
            return new AverageRatingApi(getRatingsComponent().getUserReputation(username), context);
        } catch (Throwable e) {
            context.put("exception", e);
            return null;
        }
    }

    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }
}
