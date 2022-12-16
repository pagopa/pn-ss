
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per test_otp complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="test_otp"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="test_credential" type="{http://arubasignservice.arubapec.it/}testCredential" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "test_otp", propOrder = {
    "testCredential"
})
public class TestOtp {

    @XmlElement(name = "test_credential")
    protected TestCredential testCredential;

    /**
     * Recupera il valore della proprietà testCredential.
     * 
     * @return
     *     possible object is
     *     {@link TestCredential }
     *     
     */
    public TestCredential getTestCredential() {
        return testCredential;
    }

    /**
     * Imposta il valore della proprietà testCredential.
     * 
     * @param value
     *     allowed object is
     *     {@link TestCredential }
     *     
     */
    public void setTestCredential(TestCredential value) {
        this.testCredential = value;
    }

}
