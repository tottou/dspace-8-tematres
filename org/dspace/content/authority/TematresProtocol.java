package org.dspace.content.authority;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


/** 
 * @author Arthur.Souza tottou@gmail.com
 */

public abstract class TematresProtocol implements ChoiceAuthority{
	
	private ConfigurationService configurationService;
	
    protected static final Logger log = org.apache.logging.log4j.LogManager.getLogger(TematresProtocol.class);

    private static String url = null;

    public TematresProtocol(){
    	this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        if (url == null)
        {        	
            url = configurationService.getProperty("tematres.url");            
            if (url == null)
            {
                throw new IllegalStateException("Inserir a url Tematres no dspacec.fg tematres.url");
            }
        }
    }

    public abstract Choices getMatches(String text, int collection, int start, int limit, String locale);

    @Override
    public Choices getBestMatch( String text, String locale){
        return getMatches(text,  0, 2, locale);
    }

    @Override
    public String getLabel(String key, String locale){
        return key;
    }   
    
   
	protected Choices query(String result, String label, String authority, List<BasicNameValuePair> args, int start, int limit) {

	String srUrl = url + "?" + URLEncodedUtils.format(args, "UTF-8");
	
	log.debug("Buscando termos Tematres, URL=" + srUrl);
	
	HttpURLConnection connection = null;
	try {
	    URL urlObj = new URL(srUrl);
	    connection = (HttpURLConnection) urlObj.openConnection();
	    connection.setRequestMethod("GET");
	    
	    int responseCode = connection.getResponseCode();
	    if (responseCode == 200) {
	        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
	            String responseBody = in.lines().collect(Collectors.joining("\n"));
	            
	            SAXParserFactory spf = SAXParserFactory.newInstance();
	            SAXParser sp = spf.newSAXParser();
	            XMLReader xr = sp.getXMLReader();
	            TematresHandler handler = new TematresHandler(result, label, authority);
	
	            xr.setFeature("http://xml.org/sax/features/namespaces", true);
	            xr.setContentHandler(handler);
	            xr.setErrorHandler(handler);
	            xr.parse(new InputSource(new java.io.StringReader(responseBody)));
	            
	            int confidence;
	            if (handler.total == 0) {
	                confidence = Choices.CF_NOTFOUND;
	            } else if (handler.total == 1) {
	                confidence = Choices.CF_UNCERTAIN;
	            } else {
	                confidence = Choices.CF_AMBIGUOUS;
	            }
	            
	            return new Choices(handler.result, start, handler.total, confidence, false);
	        }
	    }
		} catch (IOException | ParserConfigurationException | SAXException e) {
		    log.error("Consulta falhou ou o parse dos dados: ", e);
		    return null;
		} finally {
		    if (connection != null) {
		        connection.disconnect();
		    }
		}
	
	return null;
	}
	
    private static class TematresHandler  extends DefaultHandler{
        private Choice result[] = null;
        int rindex = 0; 
        int total = 0;

        private String resultElement = null;

        private String labelElement = null;

        private String authorityElement = null;

        protected String textValue = null;

        public TematresHandler(String result, String label, String authority)
        {
            super();
            resultElement = result;
            labelElement = label;
            authorityElement = authority;            
        }

        @Override
        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            String newValue = new String(ch, start, length);
            if (newValue.length() > 0)
            {
                if (textValue == null)
                {
                    textValue = newValue;
                }
                else
                {
                    textValue += newValue;
                }
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName,
                                 String qName)
            throws SAXException
        {
            if (localName.equals("cant_result"))
            {
                String stotal = textValue.trim();
                if (stotal.length() > 0)
                {
                    total = Integer.parseInt(stotal);
                    result = new Choice[total];
                    if (total > 0)
                    {
                        result[0] = new Choice();
                        log.debug("Retornou "+total+" resultados.");
                    }
                }               
            }
            else if (localName.equals(resultElement))
            {
                if (++rindex < result.length)
                {
                    result[rindex] = new Choice();
                }
            }
            else if (localName.equals(labelElement) && textValue != null)
            {
                result[rindex].value = textValue.trim();
                result[rindex].label = result[rindex].value; 
            }
            else if (authorityElement != null && localName.equals(authorityElement) && textValue != null)
            {               
                result[rindex].authority = textValue.trim();                
            }
            else if (localName.equals("message") && textValue != null)
            {
                log.warn("Tematres erro: " + textValue.trim());
            }
        }

        @Override
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts)
            throws SAXException
        {
            textValue = null;
        }

        @Override
        public void error(SAXParseException exception)
            throws SAXException
        {
            throw new SAXException(exception);
        }

        @Override
        public void fatalError(SAXParseException exception)
            throws SAXException
        {
            throw new SAXException(exception);
        }
    }
}
