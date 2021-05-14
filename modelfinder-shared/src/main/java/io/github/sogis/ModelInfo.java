package io.github.sogis;

import jsinterop.annotations.JsType;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;

@JsType(isNative=true, namespace=JsPackage.GLOBAL, name="Object")
public final class ModelInfo {
    private String displayName;
    
    private String name;
    
    private String title;
    
    private String shortDescription;

    private String version;
    
    private String file;
    
    private String repository;
    
    private String issuer;
    
    private String precursorVersion;
    
    private String technicalContact;
    
    private String furtherInformation;
    
    private String md5;
    
    private String tag;
    
    private String idgeoiv;

    @JsOverlay
    public String getDisplayName() {
        return displayName;
    }

    @JsOverlay
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsOverlay
    public String getName() {
        return name;
    }

    @JsOverlay
    public void setName(String name) {
        this.name = name;
    }

    @JsOverlay
    public String getTitle() {
        return title;
    }

    @JsOverlay
    public void setTitle(String title) {
        this.title = title;
    }

    @JsOverlay
    public String getShortDescription() {
        return shortDescription;
    }

    @JsOverlay
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    @JsOverlay
    public String getVersion() {
        return version;
    }

    @JsOverlay
    public void setVersion(String version) {
        this.version = version;
    }

    @JsOverlay
    public String getFile() {
        return file;
    }

    @JsOverlay
    public void setFile(String file) {
        this.file = file;
    }

    @JsOverlay
    public String getRepository() {
        return repository;
    }

    @JsOverlay
    public void setRepository(String repository) {
        this.repository = repository;
    }

    @JsOverlay
    public String getIssuer() {
        return issuer;
    }

    @JsOverlay
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @JsOverlay
    public String getPrecursorVersion() {
        return precursorVersion;
    }

    @JsOverlay
    public void setPrecursorVersion(String precursorVersion) {
        this.precursorVersion = precursorVersion;
    }

    @JsOverlay
    public String getTechnicalContact() {
        return technicalContact;
    }
    
    @JsOverlay
    public void setTechnicalContact(String technicalContact) {
        this.technicalContact = technicalContact;
    }

    @JsOverlay
    public String getFurtherInformation() {
        return furtherInformation;
    }

    @JsOverlay
    public void setFurtherInformation(String furtherInformation) {
        this.furtherInformation = furtherInformation;
    }

    @JsOverlay
    public String getMd5() {
        return md5;
    }

    @JsOverlay
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @JsOverlay
    public String getTag() {
        return tag;
    }

    @JsOverlay
    public void setTag(String tag) {
        this.tag = tag;
    }

    @JsOverlay
    public String getIdgeoiv() {
        return idgeoiv;
    }

    @JsOverlay
    public void setIdgeoiv(String idgeoiv) {
        this.idgeoiv = idgeoiv;
    }
}
