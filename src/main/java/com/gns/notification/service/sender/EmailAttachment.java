package com.gns.notification.service.sender;

public class EmailAttachment {

    private String filename;
    private byte[] content;

    public EmailAttachment() {
    }

    public EmailAttachment(String filename, byte[] content) {
        this.filename = filename;
        this.content = content;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
