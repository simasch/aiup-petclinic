package ai.unifiedprocess.petclinic.vet.web;

import ai.unifiedprocess.petclinic.vet.domain.Vet;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/**
 * Container object for UC-002 alternative flow A1: Request Vets as JSON/XML.
 */
@JacksonXmlRootElement(localName = "vets")
public record Vets(
        @JacksonXmlProperty(localName = "vetList") @JacksonXmlElementWrapper(useWrapping = false) List<Vet> vetList) {
}
