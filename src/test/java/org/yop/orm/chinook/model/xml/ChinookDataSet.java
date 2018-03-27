//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2018.03.20 à 02:59:08 PM CET 
//


package org.yop.orm.chinook.model.xml;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Classe Java pour anonymous complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="Genre">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="GenreId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="Name" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="120"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="MediaType">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="MediaTypeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="Name" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="120"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Artist">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="ArtistId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="Name" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="120"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Album">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="AlbumId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="Title">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="160"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="ArtistId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Track">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="TrackId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="Name">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="200"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="AlbumId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *                   &lt;element name="MediaTypeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="GenreId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *                   &lt;element name="Composer" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="220"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Milliseconds" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="Bytes" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *                   &lt;element name="UnitPrice" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Employee">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="EmployeeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="LastName">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="20"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="FirstName">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="20"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Title" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="30"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="ReportsTo" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *                   &lt;element name="BirthDate" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *                   &lt;element name="HireDate" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *                   &lt;element name="Address" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="70"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="City" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="State" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Country" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="PostalCode" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="10"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Phone" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="24"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Fax" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="24"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Email" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="60"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Customer">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="CustomerId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="FirstName">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="LastName">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="20"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Company" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="80"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Address" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="70"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="City" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="State" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Country" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="PostalCode" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="10"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Phone" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="24"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Fax" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="24"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Email">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="60"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="SupportRepId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Invoice">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="InvoiceId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="CustomerId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="InvoiceDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *                   &lt;element name="BillingAddress" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="70"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="BillingCity" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="BillingState" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="BillingCountry" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="40"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="BillingPostalCode" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="10"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                   &lt;element name="Total" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="InvoiceLine">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="InvoiceLineId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="InvoiceId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="TrackId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="UnitPrice" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *                   &lt;element name="Quantity" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Playlist">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="PlaylistId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="Name" minOccurs="0">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;maxLength value="120"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="PlaylistTrack">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="PlaylistId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                   &lt;element name="TrackId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "genreOrMediaTypeOrArtist"
})
@XmlRootElement(name = "ChinookDataSet")
public class ChinookDataSet {

    @XmlElements({
        @XmlElement(name = "Genre", type = Genre.class),
        @XmlElement(name = "MediaType", type = MediaType.class),
        @XmlElement(name = "Artist", type = Artist.class),
        @XmlElement(name = "Album", type = Album.class),
        @XmlElement(name = "Track", type = Track.class),
        @XmlElement(name = "Employee", type = Employee.class),
        @XmlElement(name = "Customer", type = Customer.class),
        @XmlElement(name = "Invoice", type = Invoice.class),
        @XmlElement(name = "InvoiceLine", type = InvoiceLine.class),
        @XmlElement(name = "Playlist", type = Playlist.class),
        @XmlElement(name = "PlaylistTrack", type = PlaylistTrack.class)
    })
    protected List<Object> genreOrMediaTypeOrArtist;

    /**
     * Gets the value of the genreOrMediaTypeOrArtist property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the genreOrMediaTypeOrArtist property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGenreOrMediaTypeOrArtist().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Genre }
     * {@link MediaType }
     * {@link Artist }
     * {@link Album }
     * {@link Track }
     * {@link Employee }
     * {@link Customer }
     * {@link Invoice }
     * {@link InvoiceLine }
     * {@link Playlist }
     * {@link PlaylistTrack }
     * 
     * 
     */
    public List<Object> getGenreOrMediaTypeOrArtist() {
        if (genreOrMediaTypeOrArtist == null) {
            genreOrMediaTypeOrArtist = new ArrayList<Object>();
        }
        return this.genreOrMediaTypeOrArtist;
    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="AlbumId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="Title">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="160"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="ArtistId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "albumId",
        "title",
        "artistId"
    })
    public static class Album {

        @XmlElement(name = "AlbumId")
        protected int albumId;
        @XmlElement(name = "Title", required = true)
        protected String title;
        @XmlElement(name = "ArtistId")
        protected int artistId;

        /**
         * Obtient la valeur de la propriété albumId.
         * 
         */
        public int getAlbumId() {
            return albumId;
        }

        /**
         * Définit la valeur de la propriété albumId.
         * 
         */
        public void setAlbumId(int value) {
            this.albumId = value;
        }

        /**
         * Obtient la valeur de la propriété title.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getTitle() {
            return title;
        }

        /**
         * Définit la valeur de la propriété title.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setTitle(String value) {
            this.title = value;
        }

        /**
         * Obtient la valeur de la propriété artistId.
         * 
         */
        public int getArtistId() {
            return artistId;
        }

        /**
         * Définit la valeur de la propriété artistId.
         * 
         */
        public void setArtistId(int value) {
            this.artistId = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="ArtistId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="Name" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="120"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "artistId",
        "name"
    })
    public static class Artist {

        @XmlElement(name = "ArtistId")
        protected int artistId;
        @XmlElement(name = "Name")
        protected String name;

        /**
         * Obtient la valeur de la propriété artistId.
         * 
         */
        public int getArtistId() {
            return artistId;
        }

        /**
         * Définit la valeur de la propriété artistId.
         * 
         */
        public void setArtistId(int value) {
            this.artistId = value;
        }

        /**
         * Obtient la valeur de la propriété name.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Définit la valeur de la propriété name.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="CustomerId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="FirstName">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="LastName">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="20"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Company" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="80"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Address" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="70"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="City" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="State" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Country" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="PostalCode" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="10"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Phone" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="24"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Fax" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="24"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Email">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="60"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="SupportRepId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "customerId",
        "firstName",
        "lastName",
        "company",
        "address",
        "city",
        "state",
        "country",
        "postalCode",
        "phone",
        "fax",
        "email",
        "supportRepId"
    })
    public static class Customer {

        @XmlElement(name = "CustomerId")
        protected int customerId;
        @XmlElement(name = "FirstName", required = true)
        protected String firstName;
        @XmlElement(name = "LastName", required = true)
        protected String lastName;
        @XmlElement(name = "Company")
        protected String company;
        @XmlElement(name = "Address")
        protected String address;
        @XmlElement(name = "City")
        protected String city;
        @XmlElement(name = "State")
        protected String state;
        @XmlElement(name = "Country")
        protected String country;
        @XmlElement(name = "PostalCode")
        protected String postalCode;
        @XmlElement(name = "Phone")
        protected String phone;
        @XmlElement(name = "Fax")
        protected String fax;
        @XmlElement(name = "Email", required = true)
        protected String email;
        @XmlElement(name = "SupportRepId")
        protected Integer supportRepId;

        /**
         * Obtient la valeur de la propriété customerId.
         * 
         */
        public int getCustomerId() {
            return customerId;
        }

        /**
         * Définit la valeur de la propriété customerId.
         * 
         */
        public void setCustomerId(int value) {
            this.customerId = value;
        }

        /**
         * Obtient la valeur de la propriété firstName.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFirstName() {
            return firstName;
        }

        /**
         * Définit la valeur de la propriété firstName.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFirstName(String value) {
            this.firstName = value;
        }

        /**
         * Obtient la valeur de la propriété lastName.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getLastName() {
            return lastName;
        }

        /**
         * Définit la valeur de la propriété lastName.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setLastName(String value) {
            this.lastName = value;
        }

        /**
         * Obtient la valeur de la propriété company.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCompany() {
            return company;
        }

        /**
         * Définit la valeur de la propriété company.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCompany(String value) {
            this.company = value;
        }

        /**
         * Obtient la valeur de la propriété address.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getAddress() {
            return address;
        }

        /**
         * Définit la valeur de la propriété address.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setAddress(String value) {
            this.address = value;
        }

        /**
         * Obtient la valeur de la propriété city.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCity() {
            return city;
        }

        /**
         * Définit la valeur de la propriété city.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCity(String value) {
            this.city = value;
        }

        /**
         * Obtient la valeur de la propriété state.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getState() {
            return state;
        }

        /**
         * Définit la valeur de la propriété state.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setState(String value) {
            this.state = value;
        }

        /**
         * Obtient la valeur de la propriété country.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCountry() {
            return country;
        }

        /**
         * Définit la valeur de la propriété country.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCountry(String value) {
            this.country = value;
        }

        /**
         * Obtient la valeur de la propriété postalCode.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPostalCode() {
            return postalCode;
        }

        /**
         * Définit la valeur de la propriété postalCode.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPostalCode(String value) {
            this.postalCode = value;
        }

        /**
         * Obtient la valeur de la propriété phone.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPhone() {
            return phone;
        }

        /**
         * Définit la valeur de la propriété phone.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPhone(String value) {
            this.phone = value;
        }

        /**
         * Obtient la valeur de la propriété fax.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFax() {
            return fax;
        }

        /**
         * Définit la valeur de la propriété fax.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFax(String value) {
            this.fax = value;
        }

        /**
         * Obtient la valeur de la propriété email.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getEmail() {
            return email;
        }

        /**
         * Définit la valeur de la propriété email.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setEmail(String value) {
            this.email = value;
        }

        /**
         * Obtient la valeur de la propriété supportRepId.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getSupportRepId() {
            return supportRepId;
        }

        /**
         * Définit la valeur de la propriété supportRepId.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setSupportRepId(Integer value) {
            this.supportRepId = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="EmployeeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="LastName">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="20"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="FirstName">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="20"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Title" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="30"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="ReportsTo" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
     *         &lt;element name="BirthDate" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
     *         &lt;element name="HireDate" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
     *         &lt;element name="Address" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="70"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="City" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="State" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Country" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="PostalCode" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="10"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Phone" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="24"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Fax" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="24"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Email" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="60"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "employeeId",
        "lastName",
        "firstName",
        "title",
        "reportsTo",
        "birthDate",
        "hireDate",
        "address",
        "city",
        "state",
        "country",
        "postalCode",
        "phone",
        "fax",
        "email"
    })
    public static class Employee {

        @XmlElement(name = "EmployeeId")
        protected int employeeId;
        @XmlElement(name = "LastName", required = true)
        protected String lastName;
        @XmlElement(name = "FirstName", required = true)
        protected String firstName;
        @XmlElement(name = "Title")
        protected String title;
        @XmlElement(name = "ReportsTo")
        protected Integer reportsTo;
        @XmlElement(name = "BirthDate")
        @XmlSchemaType(name = "dateTime")
        protected XMLGregorianCalendar birthDate;
        @XmlElement(name = "HireDate")
        @XmlSchemaType(name = "dateTime")
        protected XMLGregorianCalendar hireDate;
        @XmlElement(name = "Address")
        protected String address;
        @XmlElement(name = "City")
        protected String city;
        @XmlElement(name = "State")
        protected String state;
        @XmlElement(name = "Country")
        protected String country;
        @XmlElement(name = "PostalCode")
        protected String postalCode;
        @XmlElement(name = "Phone")
        protected String phone;
        @XmlElement(name = "Fax")
        protected String fax;
        @XmlElement(name = "Email")
        protected String email;

        /**
         * Obtient la valeur de la propriété employeeId.
         * 
         */
        public int getEmployeeId() {
            return employeeId;
        }

        /**
         * Définit la valeur de la propriété employeeId.
         * 
         */
        public void setEmployeeId(int value) {
            this.employeeId = value;
        }

        /**
         * Obtient la valeur de la propriété lastName.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getLastName() {
            return lastName;
        }

        /**
         * Définit la valeur de la propriété lastName.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setLastName(String value) {
            this.lastName = value;
        }

        /**
         * Obtient la valeur de la propriété firstName.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFirstName() {
            return firstName;
        }

        /**
         * Définit la valeur de la propriété firstName.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFirstName(String value) {
            this.firstName = value;
        }

        /**
         * Obtient la valeur de la propriété title.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getTitle() {
            return title;
        }

        /**
         * Définit la valeur de la propriété title.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setTitle(String value) {
            this.title = value;
        }

        /**
         * Obtient la valeur de la propriété reportsTo.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getReportsTo() {
            return reportsTo;
        }

        /**
         * Définit la valeur de la propriété reportsTo.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setReportsTo(Integer value) {
            this.reportsTo = value;
        }

        /**
         * Obtient la valeur de la propriété birthDate.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getBirthDate() {
            return birthDate;
        }

        /**
         * Définit la valeur de la propriété birthDate.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setBirthDate(XMLGregorianCalendar value) {
            this.birthDate = value;
        }

        /**
         * Obtient la valeur de la propriété hireDate.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getHireDate() {
            return hireDate;
        }

        /**
         * Définit la valeur de la propriété hireDate.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setHireDate(XMLGregorianCalendar value) {
            this.hireDate = value;
        }

        /**
         * Obtient la valeur de la propriété address.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getAddress() {
            return address;
        }

        /**
         * Définit la valeur de la propriété address.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setAddress(String value) {
            this.address = value;
        }

        /**
         * Obtient la valeur de la propriété city.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCity() {
            return city;
        }

        /**
         * Définit la valeur de la propriété city.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCity(String value) {
            this.city = value;
        }

        /**
         * Obtient la valeur de la propriété state.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getState() {
            return state;
        }

        /**
         * Définit la valeur de la propriété state.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setState(String value) {
            this.state = value;
        }

        /**
         * Obtient la valeur de la propriété country.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCountry() {
            return country;
        }

        /**
         * Définit la valeur de la propriété country.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCountry(String value) {
            this.country = value;
        }

        /**
         * Obtient la valeur de la propriété postalCode.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPostalCode() {
            return postalCode;
        }

        /**
         * Définit la valeur de la propriété postalCode.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPostalCode(String value) {
            this.postalCode = value;
        }

        /**
         * Obtient la valeur de la propriété phone.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPhone() {
            return phone;
        }

        /**
         * Définit la valeur de la propriété phone.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPhone(String value) {
            this.phone = value;
        }

        /**
         * Obtient la valeur de la propriété fax.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFax() {
            return fax;
        }

        /**
         * Définit la valeur de la propriété fax.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFax(String value) {
            this.fax = value;
        }

        /**
         * Obtient la valeur de la propriété email.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getEmail() {
            return email;
        }

        /**
         * Définit la valeur de la propriété email.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setEmail(String value) {
            this.email = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="GenreId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="Name" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="120"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "genreId",
        "name"
    })
    public static class Genre {

        @XmlElement(name = "GenreId")
        protected int genreId;
        @XmlElement(name = "Name")
        protected String name;

        /**
         * Obtient la valeur de la propriété genreId.
         * 
         */
        public int getGenreId() {
            return genreId;
        }

        /**
         * Définit la valeur de la propriété genreId.
         * 
         */
        public void setGenreId(int value) {
            this.genreId = value;
        }

        /**
         * Obtient la valeur de la propriété name.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Définit la valeur de la propriété name.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="InvoiceId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="CustomerId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="InvoiceDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
     *         &lt;element name="BillingAddress" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="70"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="BillingCity" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="BillingState" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="BillingCountry" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="40"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="BillingPostalCode" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="10"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Total" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "invoiceId",
        "customerId",
        "invoiceDate",
        "billingAddress",
        "billingCity",
        "billingState",
        "billingCountry",
        "billingPostalCode",
        "total"
    })
    public static class Invoice {

        @XmlElement(name = "InvoiceId")
        protected int invoiceId;
        @XmlElement(name = "CustomerId")
        protected int customerId;
        @XmlElement(name = "InvoiceDate", required = true)
        @XmlSchemaType(name = "dateTime")
        protected XMLGregorianCalendar invoiceDate;
        @XmlElement(name = "BillingAddress")
        protected String billingAddress;
        @XmlElement(name = "BillingCity")
        protected String billingCity;
        @XmlElement(name = "BillingState")
        protected String billingState;
        @XmlElement(name = "BillingCountry")
        protected String billingCountry;
        @XmlElement(name = "BillingPostalCode")
        protected String billingPostalCode;
        @XmlElement(name = "Total", required = true)
        protected BigDecimal total;

        /**
         * Obtient la valeur de la propriété invoiceId.
         * 
         */
        public int getInvoiceId() {
            return invoiceId;
        }

        /**
         * Définit la valeur de la propriété invoiceId.
         * 
         */
        public void setInvoiceId(int value) {
            this.invoiceId = value;
        }

        /**
         * Obtient la valeur de la propriété customerId.
         * 
         */
        public int getCustomerId() {
            return customerId;
        }

        /**
         * Définit la valeur de la propriété customerId.
         * 
         */
        public void setCustomerId(int value) {
            this.customerId = value;
        }

        /**
         * Obtient la valeur de la propriété invoiceDate.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getInvoiceDate() {
            return invoiceDate;
        }

        /**
         * Définit la valeur de la propriété invoiceDate.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setInvoiceDate(XMLGregorianCalendar value) {
            this.invoiceDate = value;
        }

        /**
         * Obtient la valeur de la propriété billingAddress.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getBillingAddress() {
            return billingAddress;
        }

        /**
         * Définit la valeur de la propriété billingAddress.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setBillingAddress(String value) {
            this.billingAddress = value;
        }

        /**
         * Obtient la valeur de la propriété billingCity.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getBillingCity() {
            return billingCity;
        }

        /**
         * Définit la valeur de la propriété billingCity.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setBillingCity(String value) {
            this.billingCity = value;
        }

        /**
         * Obtient la valeur de la propriété billingState.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getBillingState() {
            return billingState;
        }

        /**
         * Définit la valeur de la propriété billingState.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setBillingState(String value) {
            this.billingState = value;
        }

        /**
         * Obtient la valeur de la propriété billingCountry.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getBillingCountry() {
            return billingCountry;
        }

        /**
         * Définit la valeur de la propriété billingCountry.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setBillingCountry(String value) {
            this.billingCountry = value;
        }

        /**
         * Obtient la valeur de la propriété billingPostalCode.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getBillingPostalCode() {
            return billingPostalCode;
        }

        /**
         * Définit la valeur de la propriété billingPostalCode.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setBillingPostalCode(String value) {
            this.billingPostalCode = value;
        }

        /**
         * Obtient la valeur de la propriété total.
         * 
         * @return
         *     possible object is
         *     {@link BigDecimal }
         *     
         */
        public BigDecimal getTotal() {
            return total;
        }

        /**
         * Définit la valeur de la propriété total.
         * 
         * @param value
         *     allowed object is
         *     {@link BigDecimal }
         *     
         */
        public void setTotal(BigDecimal value) {
            this.total = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="InvoiceLineId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="InvoiceId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="TrackId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="UnitPrice" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
     *         &lt;element name="Quantity" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "invoiceLineId",
        "invoiceId",
        "trackId",
        "unitPrice",
        "quantity"
    })
    public static class InvoiceLine {

        @XmlElement(name = "InvoiceLineId")
        protected int invoiceLineId;
        @XmlElement(name = "InvoiceId")
        protected int invoiceId;
        @XmlElement(name = "TrackId")
        protected int trackId;
        @XmlElement(name = "UnitPrice", required = true)
        protected BigDecimal unitPrice;
        @XmlElement(name = "Quantity")
        protected int quantity;

        /**
         * Obtient la valeur de la propriété invoiceLineId.
         * 
         */
        public int getInvoiceLineId() {
            return invoiceLineId;
        }

        /**
         * Définit la valeur de la propriété invoiceLineId.
         * 
         */
        public void setInvoiceLineId(int value) {
            this.invoiceLineId = value;
        }

        /**
         * Obtient la valeur de la propriété invoiceId.
         * 
         */
        public int getInvoiceId() {
            return invoiceId;
        }

        /**
         * Définit la valeur de la propriété invoiceId.
         * 
         */
        public void setInvoiceId(int value) {
            this.invoiceId = value;
        }

        /**
         * Obtient la valeur de la propriété trackId.
         * 
         */
        public int getTrackId() {
            return trackId;
        }

        /**
         * Définit la valeur de la propriété trackId.
         * 
         */
        public void setTrackId(int value) {
            this.trackId = value;
        }

        /**
         * Obtient la valeur de la propriété unitPrice.
         * 
         * @return
         *     possible object is
         *     {@link BigDecimal }
         *     
         */
        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        /**
         * Définit la valeur de la propriété unitPrice.
         * 
         * @param value
         *     allowed object is
         *     {@link BigDecimal }
         *     
         */
        public void setUnitPrice(BigDecimal value) {
            this.unitPrice = value;
        }

        /**
         * Obtient la valeur de la propriété quantity.
         * 
         */
        public int getQuantity() {
            return quantity;
        }

        /**
         * Définit la valeur de la propriété quantity.
         * 
         */
        public void setQuantity(int value) {
            this.quantity = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="MediaTypeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="Name" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="120"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "mediaTypeId",
        "name"
    })
    public static class MediaType {

        @XmlElement(name = "MediaTypeId")
        protected int mediaTypeId;
        @XmlElement(name = "Name")
        protected String name;

        /**
         * Obtient la valeur de la propriété mediaTypeId.
         * 
         */
        public int getMediaTypeId() {
            return mediaTypeId;
        }

        /**
         * Définit la valeur de la propriété mediaTypeId.
         * 
         */
        public void setMediaTypeId(int value) {
            this.mediaTypeId = value;
        }

        /**
         * Obtient la valeur de la propriété name.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Définit la valeur de la propriété name.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="PlaylistId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="Name" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="120"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "playlistId",
        "name"
    })
    public static class Playlist {

        @XmlElement(name = "PlaylistId")
        protected int playlistId;
        @XmlElement(name = "Name")
        protected String name;

        /**
         * Obtient la valeur de la propriété playlistId.
         * 
         */
        public int getPlaylistId() {
            return playlistId;
        }

        /**
         * Définit la valeur de la propriété playlistId.
         * 
         */
        public void setPlaylistId(int value) {
            this.playlistId = value;
        }

        /**
         * Obtient la valeur de la propriété name.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Définit la valeur de la propriété name.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="PlaylistId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="TrackId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "playlistId",
        "trackId"
    })
    public static class PlaylistTrack {

        @XmlElement(name = "PlaylistId")
        protected int playlistId;
        @XmlElement(name = "TrackId")
        protected int trackId;

        /**
         * Obtient la valeur de la propriété playlistId.
         * 
         */
        public int getPlaylistId() {
            return playlistId;
        }

        /**
         * Définit la valeur de la propriété playlistId.
         * 
         */
        public void setPlaylistId(int value) {
            this.playlistId = value;
        }

        /**
         * Obtient la valeur de la propriété trackId.
         * 
         */
        public int getTrackId() {
            return trackId;
        }

        /**
         * Définit la valeur de la propriété trackId.
         * 
         */
        public void setTrackId(int value) {
            this.trackId = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="TrackId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="Name">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="200"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="AlbumId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
     *         &lt;element name="MediaTypeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="GenreId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
     *         &lt;element name="Composer" minOccurs="0">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;maxLength value="220"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;element name="Milliseconds" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *         &lt;element name="Bytes" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
     *         &lt;element name="UnitPrice" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "trackId",
        "name",
        "albumId",
        "mediaTypeId",
        "genreId",
        "composer",
        "milliseconds",
        "bytes",
        "unitPrice"
    })
    public static class Track {

        @XmlElement(name = "TrackId")
        protected int trackId;
        @XmlElement(name = "Name", required = true)
        protected String name;
        @XmlElement(name = "AlbumId")
        protected Integer albumId;
        @XmlElement(name = "MediaTypeId")
        protected int mediaTypeId;
        @XmlElement(name = "GenreId")
        protected Integer genreId;
        @XmlElement(name = "Composer")
        protected String composer;
        @XmlElement(name = "Milliseconds")
        protected int milliseconds;
        @XmlElement(name = "Bytes")
        protected Integer bytes;
        @XmlElement(name = "UnitPrice", required = true)
        protected BigDecimal unitPrice;

        /**
         * Obtient la valeur de la propriété trackId.
         * 
         */
        public int getTrackId() {
            return trackId;
        }

        /**
         * Définit la valeur de la propriété trackId.
         * 
         */
        public void setTrackId(int value) {
            this.trackId = value;
        }

        /**
         * Obtient la valeur de la propriété name.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getName() {
            return name;
        }

        /**
         * Définit la valeur de la propriété name.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Obtient la valeur de la propriété albumId.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getAlbumId() {
            return albumId;
        }

        /**
         * Définit la valeur de la propriété albumId.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setAlbumId(Integer value) {
            this.albumId = value;
        }

        /**
         * Obtient la valeur de la propriété mediaTypeId.
         * 
         */
        public int getMediaTypeId() {
            return mediaTypeId;
        }

        /**
         * Définit la valeur de la propriété mediaTypeId.
         * 
         */
        public void setMediaTypeId(int value) {
            this.mediaTypeId = value;
        }

        /**
         * Obtient la valeur de la propriété genreId.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getGenreId() {
            return genreId;
        }

        /**
         * Définit la valeur de la propriété genreId.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setGenreId(Integer value) {
            this.genreId = value;
        }

        /**
         * Obtient la valeur de la propriété composer.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getComposer() {
            return composer;
        }

        /**
         * Définit la valeur de la propriété composer.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setComposer(String value) {
            this.composer = value;
        }

        /**
         * Obtient la valeur de la propriété milliseconds.
         * 
         */
        public int getMilliseconds() {
            return milliseconds;
        }

        /**
         * Définit la valeur de la propriété milliseconds.
         * 
         */
        public void setMilliseconds(int value) {
            this.milliseconds = value;
        }

        /**
         * Obtient la valeur de la propriété bytes.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getBytes() {
            return bytes;
        }

        /**
         * Définit la valeur de la propriété bytes.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setBytes(Integer value) {
            this.bytes = value;
        }

        /**
         * Obtient la valeur de la propriété unitPrice.
         * 
         * @return
         *     possible object is
         *     {@link BigDecimal }
         *     
         */
        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        /**
         * Définit la valeur de la propriété unitPrice.
         * 
         * @param value
         *     allowed object is
         *     {@link BigDecimal }
         *     
         */
        public void setUnitPrice(BigDecimal value) {
            this.unitPrice = value;
        }

    }

}
