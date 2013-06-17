package org.xwiki.contrib.ratings;

import org.xwiki.observation.event.Event;

public class UpdateRatingEvent implements Event
{
    private String documentName;
    
    private Rating newRating;
    
    private int oldRating;

    public String getDocumentName()
    {
        return documentName;
    }

    public void setDocumentName(String documentName)
    {
        this.documentName = documentName;
    }

    public Rating getNewRating()
    {
        return newRating;
    }

    public void setNewRating(Rating newRating)
    {
        this.newRating = newRating;
    }

    public int getOldRating()
    {
        return oldRating;
    }

    public void setOldRating(int oldRating)
    {
        this.oldRating = oldRating;
    }
   
    public UpdateRatingEvent() { 
    }
    
    public UpdateRatingEvent(String documentName, Rating newRating, int oldRating) {
        this.documentName = documentName;
        this.newRating = newRating;
        this.oldRating = oldRating;
    }


    @Override
    public boolean matches(Object arg0)
    {
        return true;
    }
}
