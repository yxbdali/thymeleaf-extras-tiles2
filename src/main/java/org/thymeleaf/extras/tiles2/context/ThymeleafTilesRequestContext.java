/*
 * =============================================================================
 * 
 *   Copyright (c) 2011-2012, The THYMELEAF team (http://www.thymeleaf.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package org.thymeleaf.extras.tiles2.context;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.context.TilesRequestContextWrapper;
import org.apache.tiles.jsp.context.JspUtil;
import org.apache.tiles.servlet.context.ExternalWriterHttpServletResponse;
import org.apache.tiles.servlet.context.ServletUtil;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.util.Validate;



/**
 * 
 * @author Daniel Fern&aacute;ndez
 *
 */
public class ThymeleafTilesRequestContext extends TilesRequestContextWrapper {


    private final TemplateEngine templateEngine;
    private final ThymeleafTilesProcessingContext processingContext;
    private final Writer writer;

    // will be lazily initialized
    private Object[] requestObjects = null;


    
    
    

    public ThymeleafTilesRequestContext(
            final TilesRequestContext enclosedRequest, final TemplateEngine templateEngine, 
            final ThymeleafTilesProcessingContext processingContext, final Writer writer) {
        
        super(enclosedRequest);
        
        Validate.notNull(templateEngine, 
                "A TemplateEngine has not been specified as a Tiles request object");
        Validate.notNull(processingContext, 
                "A Thymeleaf processing context has not been specified as a Tiles request object");
        Validate.notNull(writer, 
                "A Writer has not been specified as a request Tiles object");
        
        this.templateEngine = templateEngine;
        this.processingContext = processingContext;
        this.writer = writer;

    }


    @Override
    public void dispatch(final String path) throws IOException {
        // These dispatch/include methods are called by TemplateAttributeRenderers
        // when including a JSP attribute.
        include(path);
    }


    
    @Override
    public void include(final String path) throws IOException {
        // These dispatch/include methods are called by TemplateAttributeRenderers
        // when including a JSP attribute.

        final Object[] parentRequestObjects = super.getRequestObjects();
        if (parentRequestObjects[0] instanceof PageContext) {
            final PageContext pageContext = (PageContext) parentRequestObjects[0];
            includeJsp(pageContext, path);
        } else {
            final HttpServletRequest request = (HttpServletRequest) parentRequestObjects[0];
            final HttpServletResponse response = (HttpServletResponse) parentRequestObjects[1];
            includeServlet(request, response, path);
        }
        
    }


    
    public void includeServlet(final HttpServletRequest request, final HttpServletResponse response, final String path) 
                throws IOException {
        
        ServletUtil.setForceInclude(request, true);
        
        final RequestDispatcher requestDispatcher = request.getRequestDispatcher(path);
        if (requestDispatcher == null) {
            throw new IOException("Included path \"" + path + "\" has no associated Request Dispatcher");
        }

        try {
            
            requestDispatcher.include(
                    request, new ExternalWriterHttpServletResponse(response, getPrintWriter()));
            
        } catch (final ServletException e) {
            // Wraps servlet exception as an IOException, as preferred by Tiles
            throw ServletUtil.wrapServletException(e, "Exception thrown while including path \"" + path + "\".");
        }
        
    }

    
    
    public void includeJsp(final PageContext pageContext, final String path) throws IOException {
        
        JspUtil.setForceInclude(pageContext, true);
        
        try {
            pageContext.include(path, false);
        } catch (final ServletException e) {
            // Wraps servlet exception as an IOException, as preferred by Tiles
            throw ServletUtil.wrapServletException(e, "Exception thrown while including path \"" + path + "\".");
        }
        
    }


    
    
    @Override
    public PrintWriter getPrintWriter() throws IOException {
        // Overriden in order not to return the writer from the enclosed
        // request (probably the response one).
        
        if (this.writer instanceof PrintWriter) {
            return (PrintWriter) this.writer;
        }
        return new PrintWriter(this.writer);
        
    }


    
    
    public TemplateEngine getTemplateEngine() {
        return this.templateEngine;
    }


    
    
    public ThymeleafTilesProcessingContext getProcessingContext() {
        return this.processingContext;
    }
    
    
    
    
    @Override
    public Writer getWriter() throws IOException {
        // Overriden in order not to return the writer from the enclosed
        // request (probably the response one).
        return this.writer;
    }


    
    
    @Override
    public Object[] getRequestObjects() {
        // We will need to refurbish the request object so that template engine
        // and context are the first parameters, all the objects from the
        // enclosed request come after (which normally will be 2: request and response),
        // and then finally the writer at the end (as, even not being optional, seems
        // more natural to specify the writer at the end, after the response).
        
        if (this.requestObjects == null) {
            
            final Object[] parentRequestObjects = super.getRequestObjects();
            this.requestObjects = new Object[parentRequestObjects.length + 3];

            this.requestObjects[0] = this.templateEngine;
            this.requestObjects[1] = this.processingContext;

            for (int i = 0; i < parentRequestObjects.length; i++) {
                this.requestObjects[i + 2] = parentRequestObjects[i];
            }
            this.requestObjects[this.requestObjects.length - 1] = this.writer;
            
        }
        
        return this.requestObjects;
        
    }



}
