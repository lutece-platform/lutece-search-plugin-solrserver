/*
 * Copyright (c) 2002-2009, Mairie de Paris
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

import fr.paris.lutece.test.LuteceTestCase;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import java.util.ArrayList;
import java.util.Collection;


/**
 *
 * @url http://wiki.apache.org/solr/Solrj
 */
public class SolrServerTest extends LuteceTestCase
{
    /**
     * make sure it's well configure before running this test
     * http://localhost:8080/solrserver/solr/select/?q=*
     *
     * @throws Exception
     */
    public void testPushDoc(  ) throws Exception
    {
        SolrServer server = new CommonsHttpSolrServer( "http://localhost:8080/solrserver/solr" );

        server.deleteByQuery( "*:*" ); // delete everything!
        server.commit(  );

        SolrInputDocument doc1 = new SolrInputDocument(  );
        doc1.addField( "id", "id1", 1.0f );
        doc1.addField( "name", "doc1", 1.0f );
        doc1.addField( "price", 10 );

        SolrInputDocument doc2 = new SolrInputDocument(  );
        doc2.addField( "id", "id2", 1.0f );
        doc2.addField( "name", "doc2", 1.0f );
        doc2.addField( "price", 20 );

        Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>(  );
        docs.add( doc1 );
        docs.add( doc2 );
        server.add( docs );

        server.commit(  );
    }
}
