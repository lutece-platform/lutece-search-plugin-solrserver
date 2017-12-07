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
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;


/**
 *
 * @url http://wiki.apache.org/solr/Solrj
 */
public class SolrServerTest extends LuteceTestCase
{

    public void setUp( ) throws Exception
    {
        //Because the LuteceTestCase initializes lutece without a servletcontext
        //it needs to be done manually here
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

    public MockHttpServletRequest newSolrRequest() {
        return new MockHttpServletRequest( ) {
            @Override
            public ServletInputStream getInputStream() {
                return new DelegatingServletInputStream(StreamUtils.emptyInput()) {
                    @Override public boolean isFinished() {
                        return true;
                    }
                };
            }
        };
    }
    /**
     * @throws Exception
     */
    public void testPushDoc(  ) throws Exception
    {
        //Apparently solr needs time to start
        Thread.sleep( 1000 );

        LuteceFilter filter = FilterService.getInstance( ).getFilters( ).stream( ).filter( f ->
                "solrserver".equals( f.getName( ) )
        ).findFirst( ).get( );

        MockHttpServletResponse response;
        MockHttpServletRequest request;
        LuteceFilterChain lfc;

        response = new MockHttpServletResponse( );
        lfc = new LuteceFilterChain( );
        request = newSolrRequest( );
        request.setRequestURI( "/lutece/solrserver/solr/update" );
        request.setQueryString( "stream.body=%3Cdelete%3E%3Cquery%3E%2A:%2A%3C/query%3E%3C/delete%3E&commit=true");
        request.setServletPath( SolrServerFilter.SOLR_URI + "/update" );
        filter.getFilter( ).doFilter( request, response, lfc );

        response = new MockHttpServletResponse( );
        lfc = new LuteceFilterChain( );
        request = newSolrRequest( );
        request.setRequestURI( "/lutece/solrserver/solr/update" );
        request.setServletPath( SolrServerFilter.SOLR_URI + "/update" );
        request.setQueryString("stream.body=<add><doc><field name=\"uid\">junit1</field><field name=\"content\">junitcontent1</field></doc></add>&commit=true");
        filter.getFilter( ).doFilter( request, response, lfc );

        response = new MockHttpServletResponse( );
        lfc = new LuteceFilterChain( );
        request = newSolrRequest( );
        request.setRequestURI( "/lutece/solrserver/solr/select" );
        request.setServletPath( SolrServerFilter.SOLR_URI + "/select" );
        request.setQueryString("q=*:*&wt=json");
        filter.getFilter( ).doFilter( request, response, lfc );
        JsonNode res = new ObjectMapper().readTree( response.getContentAsString( ) );
        JsonNode responseJson = res.get( "response" );
        assertEquals( 1, responseJson.get("numFound").asInt( ) );
        JsonNode doc = res.get( "response" ).get("docs").get( 0 );
        assertEquals( "junit1", doc.get("uid").asText( ) );
        assertEquals( "junitcontent1", doc.get( "content" ).asText( ) );
    }
}
