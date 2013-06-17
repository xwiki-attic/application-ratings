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
package org.xwiki.contrib.ratings;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;

import java.util.Date;

/**
 * Wrapper around a {@link Rating}, typically returned by the {@link RatingsPluginApi} and manipulated using a scripting
 * language in the wiki.
 *
 * @version $Id$
 * @see Rating
 */
public class RatingApi
{
    /**
     * The wrapped rating.
     */
    protected Rating rating;

    /**
     * Constructor of this wrapper
     *
     * @param rating the wrapped rating
     * @param context the XWiki context
     */
    public RatingApi(Rating rating)
    {
        this.rating = rating;
    }

    /**
     * @return the wrapped rating
     */
    protected Rating getRating()
    {
        return rating;
    }

    public String getGlobalRatingId()
    {
        return rating.getGlobalRatingId();
    }

    public int getVote()
    {
        if (rating == null) {
            return 0;
        } else {
            return rating.getVote();
        }
    }

    public String getAuthor()
    {
        return rating.getAuthor();
    }

    public Date getDate()
    {
        return rating.getDate();
    }
}
