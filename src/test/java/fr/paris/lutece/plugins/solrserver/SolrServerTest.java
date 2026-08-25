/*
 * Copyright (c) 2002-2014, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.solrserver;

import fr.paris.lutece.portal.service.filter.FilterService;
import fr.paris.lutece.portal.service.filter.LuteceFilter;
import fr.paris.lutece.portal.service.filter.LuteceFilterChain;
import fr.paris.lutece.portal.service.init.AppInit;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.test.LuteceTestCase;

import org.apache.commons.io.IOUtils;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;


/**
 *
 * @url http://wiki.apache.org/solr/Solrj
 */
public class SolrServerTest extends LuteceTestCase
{
    /** Content type used to post update commands to Solr */
    private static final String CONTENT_TYPE_XML = "application/xml";

    /**
     * Initializes Lutece and installs the solrserver plugin. The LuteceTestCase initializes Lutece without a
     * servlet context, so it has to be done manually here.
     *
     * @throws Exception if the initialization fails
     */
    @Override
    public void setUp( ) throws Exception
    {
        if( _bInit )
        {
            throw new Exception( "SolrServerTest must be the one to initialize LUTECE" );
        }
        else
        {

            String _strResourcesDir = getClass( ).getResource( "/" ).toString( ).replaceFirst( "file:", "" ).replaceFirst( "target/.*", "target/lutece/" );
            System.out.println( "-------------resourcesDir------------" + _strResourcesDir );
            AppPathService.init( _strResourcesDir );

            try ( InputStream in = this.getClass( ).getResourceAsStream( "plugins.dat" ) )
            {
                try ( OutputStream out = new FileOutputStream(
                            new File( _strResourcesDir + "WEB-INF/plugins/", "plugins.dat" ) ) )
                {
                    IOUtils.copy( in, out );
                }
            }

            MockServletContext context = new MockServletContext( ) {
                @Override
                public String getRealPath(String path) {
                    return _strResourcesDir + path;
                }
            };
            AppInit.initServices( context, "/WEB-INF/conf/", null );

            _bInit = true;
            System.out.println( "Lutece services initialized" );
            PluginService.getPlugin( "solrserver" ).install( );
            System.out.println( "SolrServer installed" );
        }

        super.setUp( );
    }

    /**
     * Builds a GET request without body.
     *
     * @return the request
     */
    public MockHttpServletRequest newSolrRequest( )
    {
        return newSolrRequest( null, null );
    }

    /**
     * Builds a request for the solr filter. When a body is given, the request is a POST carrying that body,
     * because Solr disables the <code>stream.body</code> parameter by default since Solr 7.
     *
     * @param strContentType the content type of the body, ignored when strBody is null
     * @param strBody the body of the request, may be null
     * @return the request
     */
    public MockHttpServletRequest newSolrRequest( String strContentType, String strBody )
    {
        final byte [ ] bytesBody = ( strBody == null ) ? new byte [ 0 ] : strBody.getBytes( StandardCharsets.UTF_8 );

        MockHttpServletRequest request = new MockHttpServletRequest( ) {
            /**
             * {@inheritDoc} The DelegatingServletInputStream of spring-test does not implement the servlet 3.1
             * methods, so they are added here.
             */
            @Override
            public ServletInputStream getInputStream() {
                return new DelegatingServletInputStream( new ByteArrayInputStream( bytesBody ) ) {
                    @Override public boolean isFinished() {
                        try
                        {
                            return getSourceStream( ).available( ) == 0;
                        }
                        catch ( IOException e )
                        {
                            return true;
                        }
                    }
                    @Override public boolean isReady() {
                        return true;
                    }
                    @Override public void setReadListener( ReadListener listener ) {
                        throw new UnsupportedOperationException( );
                    }
                };
            }
        };

        if ( strBody != null )
        {
            request.setMethod( "POST" );
            request.setContentType( strContentType );
            request.setContent( bytesBody );
        }

        return request;
    }

    /**
     * Sends the request through the solrserver filter and returns the body of the response.
     *
     * @param filter the solrserver filter
     * @param request the request
     * @return the content of the response
     * @throws Exception if the filter fails
     */
    private String doFilter( LuteceFilter filter, MockHttpServletRequest request ) throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse( );
        System.out.println( request.getRequestURI( ) + "?" + request.getQueryString( ) );
        filter.getFilter( ).doFilter( request, response, new LuteceFilterChain( ) );

        String strResponse = response.getContentAsString( );
        System.out.println( strResponse );
        assertEquals( "Unexpected HTTP status for " + request.getRequestURI( ), MockHttpServletResponse.SC_OK,
                response.getStatus( ) );

        return strResponse;
    }

    /**
     * Sends an update command to solr and checks that it succeeded.
     *
     * @param filter the solrserver filter
     * @param strCommand the xml update command
     * @throws Exception if the update fails
     */
    private void update( LuteceFilter filter, String strCommand ) throws Exception
    {
        MockHttpServletRequest request = newSolrRequest( CONTENT_TYPE_XML, strCommand );
        request.setRequestURI( "/lutece/solrserver/solr/update" );
        request.setServletPath( SolrServerFilter.SOLR_URI + "/update" );
        request.setQueryString( "commit=true&wt=json" );

        JsonNode res = new ObjectMapper( ).readTree( doFilter( filter, request ) );
        assertEquals( "Solr update failed", 0, res.get( "responseHeader" ).get( "status" ).asInt( ) );
    }

    /**
     * Pushes a document to solr through the solrserver filter and checks that it can be retrieved.
     *
     * @throws Exception if the test fails
     */
    public void testPushDoc(  ) throws Exception
    {
        long nWait = 3000;
        System.out.println("Waiting " + nWait/1000.0 + " seconds for the solrserver to settle");
        Thread.sleep( nWait );

        LuteceFilter filter = FilterService.getInstance( ).getFilters( ).stream( ).filter( f ->
                "solrserver".equals( f.getName( ) )
        ).findFirst( ).get( );

        update( filter, "<delete><query>*:*</query></delete>" );

        update( filter,
                "<add><doc><field name=\"uid\">junit1</field><field name=\"content\">junitcontent1</field></doc></add>" );

        MockHttpServletRequest request = newSolrRequest( );
        request.setRequestURI( "/lutece/solrserver/solr/select" );
        request.setServletPath( SolrServerFilter.SOLR_URI + "/select" );
        request.setQueryString("q=*:*&wt=json");

        JsonNode res = new ObjectMapper( ).readTree( doFilter( filter, request ) );
        JsonNode responseJson = res.get( "response" );
        assertEquals( 1, responseJson.get("numFound").asInt( ) );
        JsonNode doc = responseJson.get("docs").get( 0 );
        assertEquals( "junit1", doc.get("uid").asText( ) );
        assertEquals( "junitcontent1", doc.get( "content" ).asText( ) );
    }
}
