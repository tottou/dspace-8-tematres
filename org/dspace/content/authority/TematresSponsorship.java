package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

/** 
 * @author Arthur.Souza tottou@gmail.com
 */

public class TematresSponsorship extends TematresProtocol
{
    private static final String RESULT = "term";
    private static final String LABEL = "string";
    private static final String AUTHORITY = "term_id";
    
    public TematresSponsorship()
    {
        super();
    }

    @Override
    public Choices getMatches(String text, int collection, int start, int limit, String locale)
    {
        if (text == null || text.trim().length() == 0)
        {
            return new Choices(true);
        }

        List<BasicNameValuePair> args = new ArrayList<BasicNameValuePair>();
        args.add(new BasicNameValuePair("arg", text));
        args.add(new BasicNameValuePair("task","search")); 

        Choices result = query(RESULT, LABEL, AUTHORITY, args, start, limit);
        if (result == null)
        {
            result =  new Choices(true);
        }
        return result;
    }

    @Override
    public Choices getMatches(String text,  int start, int limit, String locale) {
        return getMatches(text, 0, start, limit, locale);
    }


	@Override
	public String getPluginInstanceName() {
		return null;
	}

	@Override
	public void setPluginInstanceName(String name) {
		
	}
}
