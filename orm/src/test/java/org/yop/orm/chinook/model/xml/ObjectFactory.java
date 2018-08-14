//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2018.03.20 à 02:59:08 PM CET 
//


package org.yop.orm.chinook.model.xml;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.yop.orm.chinook package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.yop.orm.chinook
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ChinookDataSet }
     * 
     */
    public ChinookDataSet createChinookDataSet() {
        return new ChinookDataSet();
    }

    /**
     * Create an instance of {@link ChinookDataSet.Genre }
     * 
     */
    public ChinookDataSet.Genre createChinookDataSetGenre() {
        return new ChinookDataSet.Genre();
    }

    /**
     * Create an instance of {@link ChinookDataSet.MediaType }
     * 
     */
    public ChinookDataSet.MediaType createChinookDataSetMediaType() {
        return new ChinookDataSet.MediaType();
    }

    /**
     * Create an instance of {@link ChinookDataSet.Artist }
     * 
     */
    public ChinookDataSet.Artist createChinookDataSetArtist() {
        return new ChinookDataSet.Artist();
    }

    /**
     * Create an instance of {@link ChinookDataSet.Album }
     * 
     */
    public ChinookDataSet.Album createChinookDataSetAlbum() {
        return new ChinookDataSet.Album();
    }

    /**
     * Create an instance of {@link ChinookDataSet.Track }
     * 
     */
    public ChinookDataSet.Track createChinookDataSetTrack() {
        return new ChinookDataSet.Track();
    }

    /**
     * Create an instance of {@link ChinookDataSet.Employee }
     * 
     */
    public ChinookDataSet.Employee createChinookDataSetEmployee() {
        return new ChinookDataSet.Employee();
    }

    /**
     * Create an instance of {@link ChinookDataSet.Customer }
     * 
     */
    public ChinookDataSet.Customer createChinookDataSetCustomer() {
        return new ChinookDataSet.Customer();
    }

    /**
     * Create an instance of {@link ChinookDataSet.Invoice }
     * 
     */
    public ChinookDataSet.Invoice createChinookDataSetInvoice() {
        return new ChinookDataSet.Invoice();
    }

    /**
     * Create an instance of {@link ChinookDataSet.InvoiceLine }
     * 
     */
    public ChinookDataSet.InvoiceLine createChinookDataSetInvoiceLine() {
        return new ChinookDataSet.InvoiceLine();
    }

    /**
     * Create an instance of {@link ChinookDataSet.Playlist }
     * 
     */
    public ChinookDataSet.Playlist createChinookDataSetPlaylist() {
        return new ChinookDataSet.Playlist();
    }

    /**
     * Create an instance of {@link ChinookDataSet.PlaylistTrack }
     * 
     */
    public ChinookDataSet.PlaylistTrack createChinookDataSetPlaylistTrack() {
        return new ChinookDataSet.PlaylistTrack();
    }

}
