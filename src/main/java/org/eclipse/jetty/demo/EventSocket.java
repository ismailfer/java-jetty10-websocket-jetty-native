//
// ========================================================================
// Copyright (c) Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.demo;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * This EventSocket instance is forked on each new connection
 * 
 */
@Slf4j
public class EventSocket extends WebSocketAdapter
{
    private final CountDownLatch closureLatch = new CountDownLatch(1);

    
    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);

        log.info(hashCode() + ".onWebSocketConnect() << " + sess.hashCode());
        
        System.out.println("Socket Connected: " + sess);
        
        sendMessagePeriodically(sess);

    }

    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        
        log.info(hashCode() + ".onWebSocketText() << " + message);

        
        System.out.println("Received TEXT message: " + message);

        if (message.toLowerCase(Locale.US).contains("bye"))
        {
            getSession().close(StatusCode.NORMAL, "Thanks");
        }
        

    }

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode, reason);
        
        log.info(hashCode() + ".onWebSocketClose() << "  + statusCode + ":" + reason);

        
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
        closureLatch.countDown();
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }

    public void awaitClosure() throws InterruptedException
    {
        System.out.println("Awaiting closure from remote");

        closureLatch.await();
    }

    public void sendMessagePeriodically(Session sess)
    {
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                int msgCount=0;
                
                try
                {
                    while (true)
                    {
                        
                        if (sess.isOpen())
                        {
                            msgCount++;
                            
                            StringBuilder sb = new StringBuilder();
                            sb.append("{");
                            sb.append("\"socket\":\"" + hashCode() + "\"");
                            sb.append(",");
                            sb.append("\"session\":\"" + sess.hashCode() + "\"");
                            sb.append(",");
                            sb.append("\"msg\":\"" + msgCount + "\"");
                            sb.append("}");
                            
                            log.info(hashCode() + ".sendMessagePeriodically() >> " + sess.hashCode() + ": " + sb.toString());
                            
                            sess.getRemote().sendString(sb.toString());
                        }
                        else
                        {
                            break;
                        }
                        
                        sleep(2000L);
                    }
                    
                    
                } catch (Throwable tt)
                {
                    tt.printStackTrace();
                }
            }
        };

        t.setDaemon(true);
        t.start();
    }

}
