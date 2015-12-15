package com.sap.cloud.sample.mail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import javax.annotation.Resource;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;






/**
 * Servlet implementing a mail example which shows how to use the connectivity service APIs to send e-mail.
 * The example provides a simple UI to compose an e-mail message and send it. The post method uses
 * the connectivity service and the javax.mail API to send the e-mail.
 */
@WebServlet("/MailServlet")
@MultipartConfig(location="/tmp", fileSizeThreshold=1024*1024, 
maxFileSize=1024*1024*5, maxRequestSize=1024*1024*5*5)
public class MailServlet extends HttpServlet {

    @Resource(name = "mail/SAPInternalNWCloudSession")
    private Session mailSession;

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(MailServlet.class);

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Show input form to user
        response.setHeader("Content-Type", "text/html");
        PrintWriter writer = response.getWriter();
        writer.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
                + "\"http://www.w3.org/TR/html4/loose.dtd\">");
        writer.write("<html><head><title>Mail Test</title></head><body>");
        writer.write("<form action='' method='post'>");
        writer.write("<table style='width: 100%'>");
        writer.write("<tr>");
        writer.write("<td width='100px'><label>From:</label></td>");
        writer.write("<td><input type='text' size='50' value='' name='fromaddress'></td>");
        writer.write("</tr>");
        writer.write("<tr>");
        writer.write("<td><label>To:</label></td>");
        writer.write("<td><input type='text' size='50' value='' name='toaddress'></td>");
        writer.write("</tr>");
        writer.write("<tr>");
        writer.write("<td><label>Subject:</label></td>");
        writer.write("<td><textarea rows='1' cols='100' name='subjecttext'>Subject</textarea></td>");
        writer.write("</tr>");
        writer.write("<tr>");
        writer.write("<td><label>Mail:</label></td>");
        writer.write("<td><textarea rows='7' cols='100' name='mailtext'>Mail Text</textarea></td>");
        writer.write("</tr>");
        writer.write("<tr>");
        writer.write("<tr>");
        writer.write("<td><input type='submit' value='Send Mail'></td>");
        writer.write("</tr>");
        writer.write("</table>");
        writer.write("</form>");
        writer.write("</body></html>");
    }

    /** {@inheritDoc} */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        Transport transport = null;
        try {

            String from = request.getParameter("fromaddress");
            String to = request.getParameter("toaddress");
            String sub = request.getParameter("subjecttext");
            String body = request.getParameter("mailtext");
            Part log = request.getPart("trace");
            String console = request.getParameter("console"); 
            Part screenShot = request.getPart("screenShot"); 
        	if (from.isEmpty() || to.isEmpty()) {
                throw new RuntimeException("Form parameters From and To may not be empty!");
            }
            // Construct message from parameters
            MimeMessage mimeMessage = new MimeMessage(mailSession);
            InternetAddress[] fromAddress = InternetAddress.parse(from);
            InternetAddress[] toAddresses = InternetAddress.parse(to);
            mimeMessage.setFrom(fromAddress[0]);
            mimeMessage.setRecipients(RecipientType.TO, toAddresses);
            mimeMessage.setSubject(sub, "UTF-8");
            //mimeMessage.setContent(mp);
            MimeMultipart multiPart = new MimeMultipart("alternative");
            MimeBodyPart part = new MimeBodyPart();
            part.setText(body, "utf-8", "plain");
            multiPart.addBodyPart(part); 
            
            InputStream partInputStream=log.getInputStream();
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            byte[] chunk=new byte[4096];
            int amountRead;
            while ((amountRead=partInputStream.read(chunk)) != -1) {
              outputStream.write(chunk,0,amountRead);
            }
            
            
            
           // byte[] attachmentData = log.getInputStream().;  // ...h
            MimeBodyPart logAttachment = new MimeBodyPart();
            String attachmentDataStream = outputStream.toString();
            if(attachmentDataStream.length() > 0){
            	logAttachment.setFileName("trace.txt");
            	logAttachment.setContent(attachmentDataStream, "text/plain");
            	multiPart.addBodyPart(logAttachment);
            }
            
            MimeBodyPart consoleAttachment = new MimeBodyPart();
            consoleAttachment.setFileName("console.txt");
            consoleAttachment.setContent(console, "text/plain");
            multiPart.addBodyPart(consoleAttachment);
      
            InputStream partInputStream1=screenShot.getInputStream();
            ByteArrayOutputStream outputStream1=new ByteArrayOutputStream();
            while ((amountRead=partInputStream1.read(chunk)) != -1) {
              outputStream1.write(chunk,0,amountRead);
            }
            MimeBodyPart screenShotAttachment = new MimeBodyPart();
            String screenShotattachmentDataStream = outputStream1.toString();
            if(screenShotattachmentDataStream.length() > 0){
            	screenShotAttachment.setFileName("screenShot.png");
            	screenShotAttachment.setContent(screenShotattachmentDataStream, "image/png");
            	multiPart.addBodyPart(screenShotAttachment);
            }
            multiPart.addBodyPart(part);
            mimeMessage.setContent(multiPart);
            // Send mail
            transport = mailSession.getTransport();
            transport.connect();
            transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());

            // Confirm mail sending
            response.getWriter().println(
                    "E-mail was sent successfully"
                            + " from: " + from + " to :" + to );
        } catch (Exception e) {
            LOGGER.error("Mail operation failed", e);
            throw new ServletException(e);
        } finally {
            // Close transport layer
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    throw new ServletException(e);
                }
            }
        }
    }
    
}
