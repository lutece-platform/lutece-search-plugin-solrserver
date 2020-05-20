/*
 * Copyright (c) 2002-2020, City of Paris
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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.solr.servlet.SolrDispatchFilter;

import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AdminUserService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

public class SolrServerFilter extends SolrDispatchFilter
{
    public static final String SOLR_DATA_DIR = "solr.data.dir";
    public static final String SOLR_HOME_LABEL = "solr.solr.home";
    public static final String SOLR_URI = AppPropertiesService.getProperty( "solrserver.solr.uri" );
    public static final String SOLR_URI_UPDATE = AppPropertiesService.getProperty( "solrserver.solr.uri.update" );
    public static final String SOLR_URI_SELECT = AppPropertiesService.getProperty( "solrserver.solr.uri.select" );
    public static final String SOLR_URI_AUTOCOMPLETE = AppPropertiesService.getProperty( "solrserver.solr.uri.autoComplete" );

    public static final String SOLR_HOME = AppPropertiesService.getProperty( "solrserver.solr.home" );
    public static final String SOLR_ABSOLUTE_DATA = AppPropertiesService.getProperty( "solrserver.solr.absolute.data" );
    public static final String SOLR_RELATIVE_DATA = AppPropertiesService.getProperty( "solrserver.solr.relative.data" );
    public static final String SOLR_ADMIN_CLIENT = AppPropertiesService.getProperty( "solrserver.solr.host.client", "127.0.0.1" );
    public static final String SOLR_CORE_NAME = AppPropertiesService.getProperty( "solrserver.solr.core.name", "collection1" );

    @Override
    public void init( FilterConfig filterConfig ) throws ServletException
    {
        String realPath = filterConfig.getServletContext( ).getRealPath( "/" );

        System.setProperty( SOLR_HOME_LABEL, realPath + SOLR_HOME );

        if ( ( SOLR_ABSOLUTE_DATA == null ) || ( SOLR_ABSOLUTE_DATA.length( ) == 0 ) )
        {
            System.setProperty( SOLR_DATA_DIR, realPath + SOLR_RELATIVE_DATA );
        }
        else
        {
            System.setProperty( SOLR_DATA_DIR, SOLR_ABSOLUTE_DATA );
        }

        super.init( filterConfig );
    }

    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException
    {
        String strURI = ( (HttpServletRequest) request ).getRequestURI( );
        boolean bCallSolr = false;

        if ( strURI.indexOf( SOLR_URI_UPDATE ) > -1 )
        {
            AdminUser adminUser = AdminUserService.getAdminUser( (HttpServletRequest) request );
            String strRemoteAddr = ( (HttpServletRequest) request ).getRemoteAddr( );

            if ( ( adminUser != null ) || ( strRemoteAddr.compareTo( SOLR_ADMIN_CLIENT ) == 0 ) )
            {
                bCallSolr = true;
            }
        }
        else
            if ( strURI.indexOf( SOLR_URI_SELECT ) > -1 )
            {
                bCallSolr = true;
            }
            else
                if ( strURI.indexOf( SOLR_URI_AUTOCOMPLETE ) > -1 )
                {
                    bCallSolr = true;
                }

        if ( bCallSolr )
        {
            request = new HttpServletRequestWrapper( (HttpServletRequest) request )
            {
                @Override
                public String getServletPath( )
                {
                    String path = ( (HttpServletRequest) getRequest( ) ).getServletPath( );
                    path = path.substring( SOLR_URI.length( ) );
                    path = "/" + SOLR_CORE_NAME + path;

                    return path;
                }
            };
            super.doFilter( request, response, chain );
        }

    }
}
